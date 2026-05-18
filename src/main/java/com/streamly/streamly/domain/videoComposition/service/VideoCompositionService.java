package com.streamly.streamly.domain.videoComposition.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamly.streamly.domain.video.dto.AiFetchResponse;
import com.streamly.streamly.domain.video.dto.VideoComposeMessage;
import com.streamly.streamly.domain.videoComposition.entity.VideoComposition;
import com.streamly.streamly.domain.videoComposition.repository.VideoCompositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoCompositionService {

    private final VideoCompositionRepository videoCompositionRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public Long initialSave(VideoComposeMessage message) {
        if (message.getVideoId() == null || message.getObjectPrompt() == null
                || message.getStartTime() == null || message.getDuration() == null) {
            throw new IllegalArgumentException("합성 요청 필수값이 누락되었습니다.");
        }
        VideoComposition composition = VideoComposition.builder()
                .videoId(message.getVideoId())
                .objectPrompt(message.getObjectPrompt())
                .startTime(message.getStartTime())
                .duration(message.getDuration())
                .build();
        return videoCompositionRepository.save(composition).getId();
    }

    @Transactional
    public void handleCallback(Long compositionId, AiFetchResponse response) {
        VideoComposition composition = videoCompositionRepository.findById(compositionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "VideoComposition을 찾을 수 없습니다. id=" + compositionId));

        if (composition.getStatus().isTerminal()) {
            log.info("중복 콜백 무시 - compositionId: {}, 현재 상태: {}", compositionId, composition.getStatus());
            return;
        }

        if (response.isSuccess()) {
            String replacedSegmentsJson = toJson(response.getReplacedSegIndices());
            composition.markCompleted(response.getTaskId(), response.getComposedPath(), replacedSegmentsJson);
            log.info("합성 완료 - compositionId: {}, taskId: {}", compositionId, response.getTaskId());
        } else {
            composition.markFailed(response.getFailReason());
            log.warn("합성 실패 - compositionId: {}, reason: {}", compositionId, response.getFailReason());
        }
    }

    @Transactional
    public void markFailed(Long compositionId, String reason) {
        videoCompositionRepository.findById(compositionId).ifPresent(composition -> {
            if (!composition.getStatus().isTerminal()) {
                composition.markFailed(reason);
            }
        });
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("JSON 직렬화 실패: {}", e.getMessage());
            return value.toString();
        }
    }
}
