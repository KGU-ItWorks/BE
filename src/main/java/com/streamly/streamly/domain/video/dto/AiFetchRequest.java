package com.streamly.streamly.domain.video.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiFetchRequest {

    @JsonProperty("video_id")
    private Long videoId;

    @JsonProperty("video_url")
    private String videoUrl;

    @JsonProperty("start_time")
    private String startTime;

    @JsonProperty("duration")
    private Integer duration;

    public static AiFetchRequest from(Long videoId, String videoUrl, String startTime, Integer duration) {
        return AiFetchRequest.builder()
                .videoId(videoId)
                .videoUrl(videoUrl)
                .startTime(startTime)
                .duration(duration)
                .build();
    }
}
