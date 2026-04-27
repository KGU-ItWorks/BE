package com.streamly.streamly.domain.advertiser.dto;

import lombok.*;

import java.io.Serializable;

/**
 * RabbitMQ로 AI 서버에 전달하는 누끼 처리 요청 메시지
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdNukiMessage implements Serializable {
    private Long adVideoId;
    private String filePath;      // 로컬 저장된 광고 영상 경로
    private String callbackUrl;   // AI 처리 완료 후 BE에 콜백할 URL
}
