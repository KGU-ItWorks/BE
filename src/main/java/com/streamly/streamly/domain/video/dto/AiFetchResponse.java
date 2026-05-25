package com.streamly.streamly.domain.video.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiFetchResponse {
    // AI 서버는 스네이크 케이스 사용
    @JsonProperty("task_id")
    private String taskId;

    @JsonProperty("video_id")
    private Long videoId;

    @JsonProperty("ad_video_id")
    private Long adVideoId;

    @JsonProperty("object_prompt")
    private String objectPrompt;

    @JsonProperty("start_time")
    private String startTime;

    @JsonProperty("duration")
    private Integer duration;

    @JsonProperty("composed_path")
    private String composedPath;

    @JsonProperty("replaced_seg_indices")
    private List<Integer> replacedSegIndices;

    @JsonProperty("message")
    private String message;

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("fail_reason")
    private String failReason;

    @JsonProperty("result_dir")
    private String resultDir;
}
