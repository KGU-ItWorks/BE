package com.streamly.streamly.domain.videoComposition.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "video_compositions")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class VideoComposition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "video_id", nullable = false)
    private Long videoId;

    @Column(name = "ad_video_id")
    private Long adVideoId;

    // Denormalized from AdVideo.advertiser — set on markCompleted() for fast queries
    @Column(name = "advertiser_id")
    private Long advertiserId;

    @Column(name = "task_id")
    private String taskId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "bounding_box", columnDefinition = "json", nullable = false)
    private String boundingBox;

    @Column(name = "start_time", nullable = false)
    private String startTime;

    @Column(name = "duration", nullable = false)
    private Integer duration;

    @Column(name = "composed_path")
    private String composedPath;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private String replacedSegments;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @CreationTimestamp
    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "last_heartbeat_at")
    private LocalDateTime lastHeartbeatAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CompositionStatus status = CompositionStatus.QUEUED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AdvertiserApprovalStatus approvalStatus = AdvertiserApprovalStatus.PENDING;

    public void markProcessing() {
        this.status = CompositionStatus.PROCESSING;
        this.lastHeartbeatAt = LocalDateTime.now();
    }

    public void updateHeartbeat() {
        this.lastHeartbeatAt = LocalDateTime.now();
    }

    public void markCompleted(String taskId, Long adVideoId, Long advertiserId, String composedPath, String replacedSegments) {
        this.taskId = taskId;
        this.adVideoId = adVideoId;
        this.advertiserId = advertiserId;
        this.composedPath = composedPath;
        this.replacedSegments = replacedSegments;
        this.status = CompositionStatus.COMPLETED;
    }

    public void markFailed(String reason) {
        this.status = CompositionStatus.FAILED;
        this.rejectionReason = reason;
    }

    public void approve() {
        approvalStatus = AdvertiserApprovalStatus.APPROVED;
        publishedAt = LocalDateTime.now();
    }

    public void reject(String reason) {
        approvalStatus = AdvertiserApprovalStatus.REJECTED;
        this.rejectionReason = reason;
    }
}
