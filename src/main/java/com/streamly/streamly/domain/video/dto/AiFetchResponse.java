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
public class AiFetchResponse {
    // AI 서버는 스네이크 케이스 사용
    @JsonProperty("task_id")
    private String taskId;

    @JsonProperty("message")
    private String message;

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("fail_reason")
    private String failReason;

    @JsonProperty("result_dir")
    private String resultDir;
}
