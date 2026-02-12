package com.streamly.streamly.domain.video.service;

import com.streamly.streamly.domain.video.dto.VideoResponse;
import com.streamly.streamly.domain.video.entity.ApprovalStatus;
import com.streamly.streamly.domain.video.entity.Video;
import com.streamly.streamly.domain.video.entity.VideoStatus;
import com.streamly.streamly.domain.video.repository.VideoRepository;
import com.streamly.streamly.global.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminVideoService {

    private final VideoRepository videoRepository;
    private final FileStorageUtil fileStorageUtil;
    private final com.streamly.streamly.global.service.S3Service s3Service;

    private static final String ENCODED_DIRECTORY = "encoded";

    /**
     * 전체 영상 목록 조회 (필터링 가능)
     */
    @Transactional(readOnly = true)
    public Page<VideoResponse> getAllVideos(String status, String approvalStatus, Pageable pageable) {
        Page<Video> videos;

        if (status != null && approvalStatus != null) {
            VideoStatus videoStatus = VideoStatus.valueOf(status);
            ApprovalStatus approval = ApprovalStatus.valueOf(approvalStatus);
            videos = videoRepository.findByStatusAndApprovalStatus(videoStatus, approval, pageable);
        } else if (status != null) {
            VideoStatus videoStatus = VideoStatus.valueOf(status);
            videos = videoRepository.findByStatus(videoStatus, pageable);
        } else if (approvalStatus != null) {
            ApprovalStatus approval = ApprovalStatus.valueOf(approvalStatus);
            videos = videoRepository.findByApprovalStatus(approval, pageable);
        } else {
            videos = videoRepository.findAll(pageable);
        }

        return videos.map(VideoResponse::from);
    }

    /**
     * 영상 승인
     */
    @Transactional
    public VideoResponse approveVideo(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("영상을 찾을 수 없습니다."));

        if (video.getApprovalStatus() == ApprovalStatus.APPROVED) {
            throw new IllegalStateException("이미 승인된 영상입니다.");
        }

        video.approve();
        Video approvedVideo = videoRepository.save(video);

        log.info("영상 승인 완료 - ID: {}, Title: {}", videoId, video.getTitle());

        return VideoResponse.from(approvedVideo);
    }

    /**
     * 영상 거부
     */
    @Transactional
    public VideoResponse rejectVideo(Long videoId, String reason) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("영상을 찾을 수 없습니다."));

        if (video.getApprovalStatus() == ApprovalStatus.REJECTED) {
            throw new IllegalStateException("이미 거부된 영상입니다.");
        }

        video.reject(reason);
        Video rejectedVideo = videoRepository.save(video);

        log.info("영상 거부 완료 - ID: {}, Title: {}, Reason: {}", videoId, video.getTitle(), reason);

        return VideoResponse.from(rejectedVideo);
    }

    /**
     * 영상 강제 삭제 (로컬, S3, DB 모두 삭제)
     */
    @Transactional
    public void forceDeleteVideo(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("영상을 찾을 수 없습니다."));

        log.info("영상 강제 삭제 시작 - ID: {}, Title: {}", videoId, video.getTitle());

        // 1. 로컬 원본 파일 삭제
        deleteLocalOriginalFile(video);

        // 2. 로컬 인코딩된 파일 삭제
        deleteLocalEncodedFiles(video);

        // 3. 로컬 썸네일 삭제
        deleteLocalThumbnail(video);

        // 4. S3 파일 삭제
        deleteS3Files(video);

        // 5. DB에서 완전히 삭제
        videoRepository.delete(video);

        log.info("영상 강제 삭제 완료 - ID: {}, Title: {}", videoId, video.getTitle());
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
            String encodedPath = ENCODED_DIRECTORY + "/" + video.getId();
            java.nio.file.Path encodedDir = java.nio.file.Paths.get(encodedPath);

            if (java.nio.file.Files.exists(encodedDir)) {
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
     * 로컬 썸네일 삭제
     */
    private void deleteLocalThumbnail(Video video) {
        try {
            if (video.getThumbnailUrl() != null && video.getThumbnailUrl().startsWith("/thumbnails/")) {
                String thumbnailFileName = video.getThumbnailUrl().substring("/thumbnails/".length());
                String thumbnailPath = "uploads/thumbnails/" + thumbnailFileName;

                java.nio.file.Path path = java.nio.file.Paths.get(thumbnailPath);
                if (java.nio.file.Files.exists(path)) {
                    java.nio.file.Files.delete(path);
                    log.info("로컬 썸네일 삭제 완료: {}", thumbnailPath);
                }
            }
        } catch (Exception e) {
            log.warn("로컬 썸네일 삭제 실패 (계속 진행): {}", e.getMessage());
        }
    }

    /**
     * S3 파일 삭제
     */
    private void deleteS3Files(Video video) {
        try {
            if (video.getS3Key() != null || video.getCloudfrontUrl() != null) {
                String s3Prefix = "videos/" + video.getId() + "/";

                s3Service.deleteDirectory(s3Prefix);

                log.info("S3 파일 삭제 완료: {}", s3Prefix);
            }
        } catch (Exception e) {
            log.warn("S3 파일 삭제 실패 (계속 진행): {}", e.getMessage());
        }
    }

    /**
     * 대시보드 통계
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // 전체 영상 수
        long totalVideos = videoRepository.count();
        stats.put("totalVideos", totalVideos);

        // 상태별 영상 수
        long uploadingCount = videoRepository.countByStatus(VideoStatus.UPLOADING);
        long uploadedCount = videoRepository.countByStatus(VideoStatus.UPLOADED);
        long encodingCount = videoRepository.countByStatus(VideoStatus.ENCODING);
        long completedCount = videoRepository.countByStatus(VideoStatus.COMPLETED);
        long failedCount = videoRepository.countByStatus(VideoStatus.FAILED);

        stats.put("uploadingCount", uploadingCount);
        stats.put("uploadedCount", uploadedCount);
        stats.put("encodingCount", encodingCount);
        stats.put("completedCount", completedCount);
        stats.put("failedCount", failedCount);

        // 승인 상태별 영상 수
        long pendingCount = videoRepository.countByApprovalStatus(ApprovalStatus.PENDING);
        long approvedCount = videoRepository.countByApprovalStatus(ApprovalStatus.APPROVED);
        long rejectedCount = videoRepository.countByApprovalStatus(ApprovalStatus.REJECTED);

        stats.put("pendingApprovalCount", pendingCount);
        stats.put("approvedCount", approvedCount);
        stats.put("rejectedCount", rejectedCount);

        // 전체 조회수
        Long totalViews = videoRepository.sumViewCount();
        stats.put("totalViews", totalViews != null ? totalViews : 0L);

        // 전체 저장 용량 (bytes)
        Long totalStorage = videoRepository.sumOriginalFileSize();
        stats.put("totalStorageBytes", totalStorage != null ? totalStorage : 0L);
        stats.put("totalStorageGB", totalStorage != null ? totalStorage / (1024.0 * 1024.0 * 1024.0) : 0.0);

        return stats;
    }

    /**
     * 승인 대기 영상 수
     */
    @Transactional(readOnly = true)
    public Long getPendingVideosCount() {
        return videoRepository.countByApprovalStatus(ApprovalStatus.PENDING);
    }
}
