package com.streamly.streamly.domain.videoComposition.dto;

import com.streamly.streamly.domain.videoComposition.entity.CompositionStatus;
import com.streamly.streamly.domain.videoComposition.entity.VideoComposition;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VideoCompositionStatusResponse {

    private Long id;
    private Long videoId;
    private Long adVideoId;
    private CompositionStatus status;
    private String failReason;

    public static VideoCompositionStatusResponse from(VideoComposition composition) {
        return VideoCompositionStatusResponse.builder()
                .id(composition.getId())
                .videoId(composition.getVideoId())
                .adVideoId(composition.getAdVideoId())
                .status(composition.getStatus())
                .failReason(composition.getRejectionReason())
                .build();
    }
}
