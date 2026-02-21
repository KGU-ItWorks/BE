package com.streamly.streamly.domain.video.service;

import com.streamly.streamly.domain.user.entity.User;
import com.streamly.streamly.domain.user.repository.UserRepository;
import com.streamly.streamly.domain.video.dto.VideoEncodingMessage;
import com.streamly.streamly.domain.video.dto.VideoResponse;
import com.streamly.streamly.domain.video.dto.VideoUploadRequest;
import com.streamly.streamly.domain.video.entity.Video;
import com.streamly.streamly.domain.video.entity.VideoStatus;
import com.streamly.streamly.domain.video.repository.VideoRepository;
import com.streamly.streamly.global.config.RabbitMQConfig;
import com.streamly.streamly.global.exception.user.UserNotFoundException;
import com.streamly.streamly.global.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final FileStorageUtil fileStorageUtil;
    private final RabbitTemplate rabbitTemplate;
    private final com.streamly.streamly.global.service.S3Service s3Service;

    @Value("${video.encoded.directory:encoded}")
    private String encodedDirectory;

    private static final long MAX_FILE_SIZE_MB = 5000; // 5GB

    /**
     * 영상 업로드
     */
    @Transactional
    public VideoResponse uploadVideo(String email, 
                                      VideoUploadRequest request, 
                                      MultipartFile videoFile,
                                      MultipartFile thumbnailFile) {
        // 1. 사용자 조회
        User uploader = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        // 2. 파일 검증
        validateVideoFile(videoFile);

        // 3. 영상 파일 저장
        String savedFilePath = fileStorageUtil.storeFile(videoFile);

        // 4. 썸네일 파일 저장 (있는 경우)
        String thumbnailUrl = null;
        if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
            validateThumbnailFile(thumbnailFile);
            thumbnailUrl = saveThumbnail(thumbnailFile);
            log.info("Thumbnail saved: {}", thumbnailUrl);
        }

        // 5. Video 엔티티 생성
        Video video = Video.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .uploader(uploader)
                .originalFilename(videoFile.getOriginalFilename())
                .originalFileSize(videoFile.getSize())
                .originalFilePath(savedFilePath)
                .thumbnailUrl(thumbnailUrl)
                .category(request.getCategory())
                .ageRating(request.getAgeRating())
                .status(VideoStatus.UPLOADED) // 업로드 완료 상태
                .build();

        Video savedVideo = videoRepository.save(video);
        
        log.info("영상 업로드 완료 - ID: {}, Title: {}, Uploader: {}", 
                savedVideo.getId(), savedVideo.getTitle(), uploader.getEmail());

        // 인코딩 작업을 RabbitMQ에 전송
        String outputDir = encodedDirectory + "/" + savedVideo.getId();
        VideoEncodingMessage message = VideoEncodingMessage.builder()
                .videoId(savedVideo.getId())
                .originalFilePath(savedFilePath)
                .outputDirectory(outputDir)
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.VIDEO_ENCODING_EXCHANGE,
                RabbitMQConfig.VIDEO_ENCODING_ROUTING_KEY,
                message
        );
        
        log.info("Encoding message sent to RabbitMQ for video ID: {}", savedVideo.getId());

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
     * 썸네일 파일 유효성 검증
     */
    private void validateThumbnailFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return;
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("유효한 이미지 파일이 아닙니다.");
        }

        // 파일 크기 제한 (5MB)
        long maxSize = 5 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("썸네일 파일 크기는 5MB를 초과할 수 없습니다.");
        }
    }

    /**
     * 썸네일 파일 저장 (S3에 업로드)
     */
    private String saveThumbnail(MultipartFile thumbnailFile) {
        try {
            // 1. 임시 파일로 저장
            String originalFileName = thumbnailFile.getOriginalFilename();
            String uniqueFileName = java.util.UUID.randomUUID().toString() + getFileExtension(originalFileName);

            java.nio.file.Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
            java.nio.file.Path tempFile = tempDir.resolve(uniqueFileName);
            java.nio.file.Files.copy(thumbnailFile.getInputStream(), tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            try {
                // 2. S3 키 생성 (thumbnails/uuid.jpg)
                String s3Key = "thumbnails/" + uniqueFileName;

                // 3. S3에 업로드
                String thumbnailUrl = s3Service.uploadFile(tempFile.toFile(), s3Key);

                log.info("Thumbnail uploaded to S3: {} -> {}", uniqueFileName, thumbnailUrl);

                return thumbnailUrl;

            } finally {
                // 4. 임시 파일 삭제
                try {
                    java.nio.file.Files.deleteIfExists(tempFile);
                } catch (Exception e) {
                    log.warn("Failed to delete temp thumbnail file: {}", tempFile, e);
                }
            }
        } catch (Exception e) {
            log.error("썸네일 저장 실패", e);
            throw new IllegalArgumentException("썸네일 저장에 실패했습니다.");
        }
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
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
     * 영상 정보 수정
     */
    @Transactional
    public VideoResponse updateVideo(String email, Long videoId, com.streamly.streamly.domain.video.dto.VideoUpdateRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("영상을 찾을 수 없습니다."));

        // 업로더 본인 또는 관리자만 수정 가능
        if (!video.getUploader().getId().equals(user.getId()) && 
            !user.getRole().name().equals("ROLE_ADMIN")) {
            throw new IllegalArgumentException("영상을 수정할 권한이 없습니다.");
        }

        // 영상 정보 업데이트 (Builder 패턴 사용)
        video = Video.builder()
                .id(video.getId())
                .title(request.getTitle())
                .description(request.getDescription())
                .uploader(video.getUploader())
                .originalFilename(video.getOriginalFilename())
                .originalFileSize(video.getOriginalFileSize())
                .originalFilePath(video.getOriginalFilePath())
                .s3Key(video.getS3Key())
                .s3Url(video.getS3Url())
                .cloudfrontUrl(video.getCloudfrontUrl())
                .durationSeconds(video.getDurationSeconds())
                .resolution(video.getResolution())
                .videoCodec(video.getVideoCodec())
                .audioCodec(video.getAudioCodec())
                .thumbnailUrl(video.getThumbnailUrl())
                .status(video.getStatus())
                .encodingProgress(video.getEncodingProgress())
                .viewCount(video.getViewCount())
                .category(request.getCategory())
                .ageRating(request.getAgeRating())
                .approvalStatus(video.getApprovalStatus())
                .rejectionReason(video.getRejectionReason())
                .createdAt(video.getCreatedAt())
                .updatedAt(video.getUpdatedAt())
                .publishedAt(video.getPublishedAt())
                .build();

        Video updatedVideo = videoRepository.save(video);
        
        log.info("영상 정보 수정 완료 - ID: {}, Title: {}", videoId, updatedVideo.getTitle());
        
        return VideoResponse.from(updatedVideo);
    }

    /**
     * 영상 삭제 (로컬, S3, DB 모두 삭제)
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

        log.info("영상 삭제 시작 - ID: {}, Title: {}", videoId, video.getTitle());

        // 1. 로컬 원본 파일 삭제
        deleteLocalOriginalFile(video);

        // 2. 로컬 인코딩된 파일 삭제
        deleteLocalEncodedFiles(video);

        // 3. 로컬 썸네일 삭제
        deleteLocalThumbnail(video);

        // 4. S3 파일 삭제 (인코딩된 HLS 파일들)
        deleteS3Files(video);

        // 5. DB에서 완전히 삭제
        videoRepository.delete(video);

        log.info("영상 삭제 완료 - ID: {}, Title: {}", videoId, video.getTitle());
    }

    /**
     * 로컬 원본 파일 삭제
     */
    private void deleteLocalOriginalFile(Video video) {
        try {
            if (video.getOriginalFilePath() != null) {
                fileStorageUtil.deleteFile(video.getOriginalFilePath());
                log.info("로컬 원본 파일 삭제 완료: {}", video.getOriginalFilePath());
            }
        } catch (Exception e) {
            log.warn("로컬 원본 파일 삭제 실패 (계속 진행): {}", video.getOriginalFilePath(), e);
        }
    }

    /**
     * 로컬 인코딩된 파일들 삭제
     */
    private void deleteLocalEncodedFiles(Video video) {
        try {
            // encoded/{videoId}/ 디렉토리 전체 삭제
            String encodedPath = encodedDirectory + "/" + video.getId();
            java.nio.file.Path encodedDir = java.nio.file.Paths.get(encodedPath);

            if (java.nio.file.Files.exists(encodedDir)) {
                // 디렉토리 내 모든 파일 삭제
                try (java.util.stream.Stream<java.nio.file.Path> paths = java.nio.file.Files.walk(encodedDir)) {
                    paths.sorted(java.util.Comparator.reverseOrder())
                         .forEach(path -> {
                             try {
                                 java.nio.file.Files.delete(path);
                             } catch (Exception e) {
                                 log.warn("파일 삭제 실패: {}", path, e);
                             }
                         });
                }
                log.info("로컬 인코딩 파일 삭제 완료: {}", encodedPath);
            }
        } catch (Exception e) {
            log.warn("로컬 인코딩 파일 삭제 실패 (계속 진행): {}", e.getMessage());
        }
    }

    /**
     * 썸네일 삭제 (S3)
     */
    private void deleteLocalThumbnail(Video video) {
        try {
            if (video.getThumbnailUrl() != null) {
                // S3 URL인 경우 S3에서 삭제
                if (video.getThumbnailUrl().contains("s3.") || video.getThumbnailUrl().contains("cloudfront.net")) {
                    // URL에서 S3 키 추출
                    // 예: https://bucket.s3.region.amazonaws.com/thumbnails/uuid.jpg -> thumbnails/uuid.jpg
                    // 예: https://cloudfront.net/thumbnails/uuid.jpg -> thumbnails/uuid.jpg
                    String s3Key;
                    if (video.getThumbnailUrl().contains("/thumbnails/")) {
                        s3Key = "thumbnails/" + video.getThumbnailUrl().substring(video.getThumbnailUrl().lastIndexOf("/thumbnails/") + "/thumbnails/".length());
                    } else {
                        return; // S3 키를 추출할 수 없으면 스킵
                    }

                    s3Service.deleteFile(s3Key);
                    log.info("S3 썸네일 삭제 완료: {}", s3Key);
                }
                // 로컬 파일인 경우 (하위 호환성)
                else if (video.getThumbnailUrl().startsWith("/thumbnails/")) {
                    String thumbnailFileName = video.getThumbnailUrl().substring("/thumbnails/".length());
                    String thumbnailPath = "uploads/thumbnails/" + thumbnailFileName;

                    java.nio.file.Path path = java.nio.file.Paths.get(thumbnailPath);
                    if (java.nio.file.Files.exists(path)) {
                        java.nio.file.Files.delete(path);
                        log.info("로컬 썸네일 삭제 완료: {}", thumbnailPath);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("썸네일 삭제 실패 (계속 진행): {}", e.getMessage());
        }
    }

    /**
     * S3 파일 삭제
     */
    private void deleteS3Files(Video video) {
        try {
            // S3에 업로드된 파일이 있는 경우
            if (video.getS3Key() != null || video.getCloudfrontUrl() != null) {
                // videos/{videoId}/ prefix로 모든 파일 삭제
                String s3Prefix = "videos/" + video.getId() + "/";
                
                s3Service.deleteDirectory(s3Prefix);
                
                log.info("S3 파일 삭제 완료: {}", s3Prefix);
            }
        } catch (Exception e) {
            log.warn("S3 파일 삭제 실패 (계속 진행): {}", e.getMessage());
        }
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
