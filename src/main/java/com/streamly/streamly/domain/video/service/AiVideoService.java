package com.streamly.streamly.domain.video.service;

import com.streamly.streamly.domain.video.dto.VideoComposeMessage;
import com.streamly.streamly.domain.video.entity.Video;
import com.streamly.streamly.domain.video.repository.VideoRepository;
import com.streamly.streamly.domain.videoComposition.service.VideoCompositionService;
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
    private final RabbitTemplate rabbitTemplate;
    private final VideoCompositionService videoCompositionService;

    @Value("${server.base-url:http://localhost:8080}")
    private String beServerUrl;

    public void requestAiComposition(Long videoId, String startTime, Integer duration, String objectPrompt) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new VideoNotFoundException("영상을 찾을 수 없습니다. id=" + videoId));

        String rawUrl = video.getCloudfrontUrl() != null
                ? video.getCloudfrontUrl()
                : video.getS3Url();
        String videoUrl = rawUrl.startsWith("/") ? beServerUrl + rawUrl : rawUrl;

        // Build a draft message without callbackUrl to get compositionId first
        VideoComposeMessage draft = VideoComposeMessage.builder()
                .videoId(videoId)
                .videoUrl(videoUrl)
                .startTime(startTime != null ? startTime : "00:00:00")
                .duration(duration)
                .objectPrompt(objectPrompt)
                .callbackUrl("")
                .build();

        Long compositionId = videoCompositionService.initialSave(draft);

        // Now build the real message with compositionId embedded in the callback URL
        String callbackUrl = beServerUrl + "/api/v1/video-compositions/" + compositionId + "/callback";

        VideoComposeMessage message = VideoComposeMessage.builder()
                .videoId(videoId)
                .videoUrl(videoUrl)
                .startTime(draft.getStartTime())
                .duration(duration)
                .objectPrompt(objectPrompt)
                .callbackUrl(callbackUrl)
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.VIDEO_FETCH_EXCHANGE,
                RabbitMQConfig.VIDEO_FETCH_ROUTING_KEY,
                message
        );

        log.info("AI 합성 요청 - videoId: {}, compositionId: {}, callbackUrl: {}",
                videoId, compositionId, callbackUrl);
    }
}
