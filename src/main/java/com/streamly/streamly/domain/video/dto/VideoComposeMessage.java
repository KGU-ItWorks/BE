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
public class VideoComposeMessage implements Serializable {
    private Long    videoId;
    private String  videoUrl;
    private String  startTime;
    private Integer duration;
    private String  objectPrompt;
    private String  callbackUrl; // AI 처리 완료 후 BE에 콜백할 URL

    /**
     * compositionId 확보 전 초안 — callbackUrl은 빈 문자열로 저장 후 교체
     */
    public static VideoComposeMessage draft(Long videoId, String videoUrl, String startTime, Integer duration, String objectPrompt) {
        return VideoComposeMessage.builder()
                .videoId(videoId)
                .videoUrl(videoUrl)
                .startTime(startTime != null ? startTime : "00:00:00")
                .duration(duration)
                .objectPrompt(objectPrompt)
                .callbackUrl("")
                .build();
    }

    /**
     * compositionId가 확정된 후 callbackUrl을 포함한 최종 메시지
     */
    public static VideoComposeMessage withCallback(VideoComposeMessage draft, String callbackUrl) {
        return VideoComposeMessage.builder()
                .videoId(draft.videoId)
                .videoUrl(draft.videoUrl)
                .startTime(draft.startTime)
                .duration(draft.duration)
                .objectPrompt(draft.objectPrompt)
                .callbackUrl(callbackUrl)
                .build();
    }
}
