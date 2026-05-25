package com.streamly.streamly.domain.videoComposition.dto;

import com.streamly.streamly.domain.videoComposition.entity.AdvertiserApprovalStatus;
import com.streamly.streamly.domain.videoComposition.entity.VideoComposition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VideoCompositionDto {

    private Long id;
    private Long videoId;
    private Long adVideoId;
    private String taskId;
    /** HLS preview URL the advertiser can play directly. */
    private String hlsPath;
    private AdvertiserApprovalStatus approvalStatus;
    private LocalDateTime requestedAt;
    private LocalDateTime publishedAt;

    public static VideoCompositionDto from(VideoComposition c) {
        String hlsPath = null;
        if (c.getTaskId() != null) {
            hlsPath = "/encoded/" + c.getVideoId() + "/versions/" + c.getTaskId() + "/master.m3u8";
        }
        return VideoCompositionDto.builder()
                .id(c.getId())
                .videoId(c.getVideoId())
                .adVideoId(c.getAdVideoId())
                .taskId(c.getTaskId())
                .hlsPath(hlsPath)
                .approvalStatus(c.getApprovalStatus())
                .requestedAt(c.getRequestedAt())
                .publishedAt(c.getPublishedAt())
                .build();
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RejectRequest {
        private String reason;
    }
}
