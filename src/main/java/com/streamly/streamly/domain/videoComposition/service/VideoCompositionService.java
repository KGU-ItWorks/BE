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

        if (response.isSuccess()) {
            String replacedSegmentsJson = toJson(response.getReplacedSegIndices());
            composition.markCompleted(response.getTaskId(), response.getComposedPath(), replacedSegmentsJson);
            log.info("합성 완료 - compositionId: {}, taskId: {}", compositionId, response.getTaskId());
        } else {
            composition.markFailed(response.getFailReason());
            log.warn("합성 실패 - compositionId: {}, reason: {}", compositionId, response.getFailReason());
        }
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
