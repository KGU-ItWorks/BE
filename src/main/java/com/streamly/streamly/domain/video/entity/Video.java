package com.streamly.streamly.domain.video.entity;

import com.streamly.streamly.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "videos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    // 원본 파일 정보
    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "original_file_size")
    private Long originalFileSize; // bytes

    @Column(name = "original_file_path")
    private String originalFilePath; // 로컬 임시 저장 경로

    // S3 정보
    @Column(name = "s3_key")
    private String s3Key; // S3 객체 키

    @Column(name = "s3_url")
    private String s3Url; // S3 URL

    @Column(name = "cloudfront_url")
    private String cloudfrontUrl; // CloudFront CDN URL

    // 영상 메타데이터
    @Column(name = "duration_seconds")
    private Integer durationSeconds; // 영상 길이 (초)

    @Column(name = "resolution")
    private String resolution; // 해상도 (예: 1920x1080)

    @Column(name = "video_codec")
    private String videoCodec; // 비디오 코덱 (예: h264)

    @Column(name = "audio_codec")
    private String audioCodec; // 오디오 코덱 (예: aac)

    // 썸네일
    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    // 처리 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private VideoStatus status = VideoStatus.UPLOADING;

    // 인코딩 진행률 (0-100)
    @Column(name = "encoding_progress")
    @Builder.Default
    private Integer encodingProgress = 0;

    // 조회수
    @Column(name = "view_count")
    @Builder.Default
    private Long viewCount = 0L;

    // 카테고리/장르
    @Column(length = 50)
    private String category;

    @Column(length = 20)
    private String ageRating; // 연령 등급 (예: 12+, 15+)

    // 승인 상태 (관리자 검토용)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @Column(name = "rejection_reason")
    private String rejectionReason; // 거부 사유

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt; // 실제 공개 시간

    // 비즈니스 메서드
    public void updateStatus(VideoStatus status) {
        this.status = status;
    }

    public void updateEncodingProgress(Integer progress) {
        this.encodingProgress = Math.min(100, Math.max(0, progress));
    }

    public void updateS3Info(String s3Key, String s3Url, String cloudfrontUrl) {
        this.s3Key = s3Key;
        this.s3Url = s3Url;
        this.cloudfrontUrl = cloudfrontUrl;
    }

    public void updateMetadata(Integer duration, String resolution, String videoCodec, String audioCodec) {
        this.durationSeconds = duration;
        this.resolution = resolution;
        this.videoCodec = videoCodec;
        this.audioCodec = audioCodec;
    }

    public void updateThumbnail(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public void approve() {
        this.approvalStatus = ApprovalStatus.APPROVED;
        this.publishedAt = LocalDateTime.now();
    }

    public void reject(String reason) {
        this.approvalStatus = ApprovalStatus.REJECTED;
        this.rejectionReason = reason;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public boolean isPublished() {
        return this.status == VideoStatus.COMPLETED 
            && this.approvalStatus == ApprovalStatus.APPROVED;
    }
}
