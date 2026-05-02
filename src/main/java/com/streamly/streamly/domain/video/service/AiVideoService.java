package com.streamly.streamly.domain.video.service;

import com.streamly.streamly.domain.video.dto.VideoFetchMessage;
import com.streamly.streamly.domain.video.entity.Video;
import com.streamly.streamly.domain.video.repository.VideoRepository;
import com.streamly.streamly.global.config.RabbitMQConfig;
import com.streamly.streamly.global.exception.video.VideoNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiVideoService {

    private final VideoRepository videoRepository;
    private final RabbitTemplate  rabbitTemplate;

    @Value("${server.base-url:http://localhost:8080}")
    private String beServerUrl;

    public void requestAiFetch(Long videoId, String startTime, Integer duration) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new VideoNotFoundException("영상을 찾을 수 없습니다. id=" + videoId));

        String rawUrl = video.getCloudfrontUrl() != null
                ? video.getCloudfrontUrl()
                : video.getS3Url();

        String videoUrl = rawUrl.startsWith("/") ? beServerUrl + rawUrl : rawUrl;

        VideoFetchMessage message = VideoFetchMessage.builder()
                .videoId(videoId)
                .videoUrl(videoUrl)
                .startTime(startTime != null ? startTime : "00:00:00")
                .duration(duration)
                .callbackUrl(beServerUrl + "/api/v1/videos/" + videoId + "/ai-callback")
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.VIDEO_FETCH_EXCHANGE,
                RabbitMQConfig.VIDEO_FETCH_ROUTING_KEY,
                message
        );

        log.info("AI fetch 메시지 발행 - videoId: {}, url: {}", videoId, videoUrl);
    }
}

