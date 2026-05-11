package com.streamly.streamly.domain.video.dto;

import lombok.*;

import java.io.Serializable;

/**
 * RabbitMQ로 AI 서버에 전달하는 영상 fetch 요청 메시지
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoFetchMessage implements Serializable {
    private Long    videoId;
    private String  videoUrl;
    private String  startTime;
    private Integer duration;
    private String objectPrompt;
    private String  callbackUrl; // AI 처리 완료 후 BE에 콜백할 URL
}
