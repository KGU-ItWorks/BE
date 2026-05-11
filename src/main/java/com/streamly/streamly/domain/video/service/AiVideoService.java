package com.streamly.streamly.domain.video.service;

import com.streamly.streamly.domain.video.dto.AiFetchResponse;
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
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiVideoService {

    private final VideoRepository videoRepository;
    private final RabbitTemplate  rabbitTemplate;

    @Value("${server.base-url:http://localhost:8080}")
    private String beServerUrl;

    @Transactional
    public void requestAiFetch(Long videoId, String startTime, Integer duration, String objectPrompt) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new VideoNotFoundException("영상을 찾을 수 없습니다. id=" + videoId));

        video.resetSam3();
        videoRepository.save(video);

        String rawUrl = video.getCloudfrontUrl() != null
                ? video.getCloudfrontUrl()
                : video.getS3Url();

        String videoUrl = rawUrl.startsWith("/") ? beServerUrl + rawUrl : rawUrl;

        VideoFetchMessage message = VideoFetchMessage.builder()
                .videoId(videoId)
                .videoUrl(videoUrl)
                .startTime(startTime != null ? startTime : "00:00:00")
                .duration(duration)
                .objectPrompt(objectPrompt)
                .callbackUrl(beServerUrl + "/api/v1/videos/" + videoId + "/ai-callback")
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.VIDEO_FETCH_EXCHANGE,
                RabbitMQConfig.VIDEO_FETCH_ROUTING_KEY,
                message
        );

        log.info("AI fetch 메시지 발행 - videoId: {}, url: {}", videoId, videoUrl);
    }

    @Transactional
    public void handleAiCallback(Long videoId, AiFetchResponse response) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new VideoNotFoundException("영상을 찾을 수 없습니다. id=" + videoId));

        // 이미 성공한 경우만 중복 콜백 무시 — 실패 상태는 재시도 허용
        if (video.getSam3ResultDir() != null) {
            log.warn("중복 AI 콜백 수신 무시 - videoId: {}, taskId: {}", videoId, response.getTaskId());
            return;
        }

        if (response.isSuccess()) {
            video.markSam3Done(response.getResultDir());
            log.info("AI fetch 처리 완료 - videoId: {}, taskId: {}, resultDir: {}",
                    videoId, response.getTaskId(), response.getResultDir());
        } else {
            video.markSam3Failed(response.getFailReason());
            log.warn("AI fetch 처리 실패 - videoId: {}, taskId: {}, reason: {}",
                    videoId, response.getTaskId(), response.getFailReason());
        }

        videoRepository.save(video);
    }
}

