package com.streamly.streamly.domain.video.dto;

import com.streamly.streamly.domain.video.entity.ApprovalStatus;
import com.streamly.streamly.domain.video.entity.Video;
import com.streamly.streamly.domain.video.entity.VideoStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoResponse {

    private Long id;
    private String title;
    private String description;
    private String uploaderName;
    private Long uploaderId;
    
    private String originalFilename;
    private Long originalFileSize;
    
    private String s3Url; // S3 직접 URL
    private String cloudfrontUrl; // CloudFront URL (스트리밍 추천)
    private String thumbnailUrl;
    
    private Integer durationSeconds;
    private String resolution;
    private String videoCodec;
    private String audioCodec;
    
    private VideoStatus status;
    private Integer encodingProgress;
    
    private Long viewCount;
    private String category;
    private String ageRating;
    
    private ApprovalStatus approvalStatus;
    private String rejectionReason;
    
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;

    public static VideoResponse from(Video video) {
        return VideoResponse.builder()
                .id(video.getId())
                .title(video.getTitle())
                .description(video.getDescription())
                .uploaderName(video.getUploader().getNickname())
                .uploaderId(video.getUploader().getId())
                .originalFilename(video.getOriginalFilename())
                .originalFileSize(video.getOriginalFileSize())
                .s3Url(video.getS3Url())
                .cloudfrontUrl(video.getCloudfrontUrl())
                .thumbnailUrl(video.getThumbnailUrl())
                .durationSeconds(video.getDurationSeconds())
                .resolution(video.getResolution())
                .videoCodec(video.getVideoCodec())
                .audioCodec(video.getAudioCodec())
                .status(video.getStatus())
                .encodingProgress(video.getEncodingProgress())
                .viewCount(video.getViewCount())
                .category(video.getCategory())
                .ageRating(video.getAgeRating())
                .approvalStatus(video.getApprovalStatus())
                .rejectionReason(video.getRejectionReason())
                .createdAt(video.getCreatedAt())
                .publishedAt(video.getPublishedAt())
                .build();
    }

    // 간단한 정보만 포함 (목록 조회용)
    public static VideoResponse fromSimple(Video video) {
        return VideoResponse.builder()
                .id(video.getId())
                .title(video.getTitle())
                .description(video.getDescription())
                .uploaderName(video.getUploader().getNickname())
                .uploaderId(video.getUploader().getId())
                .s3Url(video.getS3Url())
                .cloudfrontUrl(video.getCloudfrontUrl())
                .thumbnailUrl(video.getThumbnailUrl())
                .durationSeconds(video.getDurationSeconds())
                .resolution(video.getResolution())
                .status(video.getStatus())
                .encodingProgress(video.getEncodingProgress())
                .viewCount(video.getViewCount())
                .category(video.getCategory())
                .ageRating(video.getAgeRating())
                .approvalStatus(video.getApprovalStatus())
                .createdAt(video.getCreatedAt())
                .publishedAt(video.getPublishedAt())
                .build();
    }
}
