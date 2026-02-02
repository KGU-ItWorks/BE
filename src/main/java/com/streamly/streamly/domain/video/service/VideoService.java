package com.streamly.streamly.domain.video.service;

import com.streamly.streamly.domain.user.entity.User;
import com.streamly.streamly.domain.user.repository.UserRepository;
import com.streamly.streamly.domain.video.dto.VideoResponse;
import com.streamly.streamly.domain.video.dto.VideoUploadRequest;
import com.streamly.streamly.domain.video.entity.Video;
import com.streamly.streamly.domain.video.entity.VideoStatus;
import com.streamly.streamly.domain.video.repository.VideoRepository;
import com.streamly.streamly.global.exception.user.UserNotFoundException;
import com.streamly.streamly.global.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final FileStorageUtil fileStorageUtil;

    private static final long MAX_FILE_SIZE_MB = 5000; // 5GB

    /**
     * 영상 업로드
     */
    @Transactional
    public VideoResponse uploadVideo(String email, 
                                      VideoUploadRequest request, 
                                      MultipartFile videoFile) {
        // 1. 사용자 조회
        User uploader = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        // 2. 파일 검증
        validateVideoFile(videoFile);

        // 3. 파일 저장
        String savedFilePath = fileStorageUtil.storeFile(videoFile);

        // 4. Video 엔티티 생성
        Video video = Video.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .uploader(uploader)
                .originalFilename(videoFile.getOriginalFilename())
                .originalFileSize(videoFile.getSize())
                .originalFilePath(savedFilePath)
                .category(request.getCategory())
                .ageRating(request.getAgeRating())
                .status(VideoStatus.UPLOADED) // 업로드 완료 상태
                .build();

        Video savedVideo = videoRepository.save(video);
        
        log.info("영상 업로드 완료 - ID: {}, Title: {}, Uploader: {}", 
                savedVideo.getId(), savedVideo.getTitle(), uploader.getEmail());

        // TODO: 비동기로 인코딩 작업 큐에 추가
        // encodingService.addToQueue(savedVideo.getId());

        return VideoResponse.from(savedVideo);
    }

    /**
     * 영상 파일 검증
     */
    private void validateVideoFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        if (!fileStorageUtil.isVideoFile(file)) {
            throw new IllegalArgumentException("영상 파일만 업로드할 수 있습니다.");
        }

        if (!fileStorageUtil.isValidFileSize(file, MAX_FILE_SIZE_MB)) {
            throw new IllegalArgumentException(
                String.format("파일 크기는 %dGB를 초과할 수 없습니다.", MAX_FILE_SIZE_MB / 1024)
            );
        }
    }

    /**
     * 공개된 영상 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<VideoResponse> getPublishedVideos(Pageable pageable) {
        return videoRepository.findPublishedVideos(pageable)
                .map(VideoResponse::fromSimple);
    }

    /**
     * 영상 상세 조회
     */
    @Transactional
    public VideoResponse getVideoById(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("영상을 찾을 수 없습니다."));

        // 조회수 증가
        video.incrementViewCount();

        return VideoResponse.from(video);
    }

    /**
     * 카테고리별 영상 조회
     */
    @Transactional(readOnly = true)
    public Page<VideoResponse> getVideosByCategory(String category, Pageable pageable) {
        return videoRepository.findPublishedVideosByCategory(category, pageable)
                .map(VideoResponse::fromSimple);
    }

    /**
     * 영상 검색
     */
    @Transactional(readOnly = true)
    public Page<VideoResponse> searchVideos(String keyword, Pageable pageable) {
        return videoRepository.searchPublishedVideos(keyword, pageable)
                .map(VideoResponse::fromSimple);
    }

    /**
     * 내가 업로드한 영상 목록
     */
    @Transactional(readOnly = true)
    public Page<VideoResponse> getMyVideos(String email, Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        return videoRepository.findByUploaderId(user.getId(), pageable)
                .map(VideoResponse::from);
    }

    /**
     * 영상 삭제
     */
    @Transactional
    public void deleteVideo(String email, Long videoId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("영상을 찾을 수 없습니다."));

        // 업로더 본인 또는 관리자만 삭제 가능
        if (!video.getUploader().getId().equals(user.getId()) && 
            !user.getRole().name().equals("ROLE_ADMIN")) {
            throw new IllegalArgumentException("영상을 삭제할 권한이 없습니다.");
        }

        // 파일 삭제
        if (video.getOriginalFilePath() != null) {
            fileStorageUtil.deleteFile(video.getOriginalFilePath());
        }

        // TODO: S3에서도 삭제

        video.updateStatus(VideoStatus.DELETED);
        
        log.info("영상 삭제 완료 - ID: {}, Title: {}", videoId, video.getTitle());
    }

    /**
     * 조회수 상위 영상
     */
    @Transactional(readOnly = true)
    public Page<VideoResponse> getTopViewedVideos(Pageable pageable) {
        return videoRepository.findTopViewedVideos(pageable)
                .map(VideoResponse::fromSimple);
    }

    /**
     * 최신 영상
     */
    @Transactional(readOnly = true)
    public Page<VideoResponse> getRecentVideos(Pageable pageable) {
        return videoRepository.findRecentVideos(pageable)
                .map(VideoResponse::fromSimple);
    }
}
