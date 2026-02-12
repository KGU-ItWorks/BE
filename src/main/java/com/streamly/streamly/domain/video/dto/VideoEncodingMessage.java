package com.streamly.streamly.domain.video.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * RabbitMQ 메시지로 전송될 인코딩 작업 정보
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoEncodingMessage implements Serializable {

    private Long videoId;
    private String originalFilePath;  // 업로드된 원본 파일 경로
    private String outputDirectory;   // 인코딩 결과물이 저장될 디렉토리
}
