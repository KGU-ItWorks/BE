package com.streamly.streamly.domain.videoComposition.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamly.streamly.domain.video.dto.AiFetchResponse;
import com.streamly.streamly.domain.video.dto.VideoComposeMessage;
import com.streamly.streamly.domain.videoComposition.dto.VideoCompositionStatusResponse;
import com.streamly.streamly.domain.videoComposition.entity.CompositionStatus;
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
    private final HeartbeatStore heartbeatStore;
    private final CompositionStatusPromoter statusPromoter;
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
        Long id = videoCompositionRepository.save(composition).getId();
        heartbeatStore.trackQueued(id);
        return id;
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

        heartbeatStore.remove(compositionId);

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
    public void markProcessing(Long compositionId) {
        VideoComposition composition = videoCompositionRepository.findById(compositionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "VideoComposition을 찾을 수 없습니다. id=" + compositionId));
        if (!composition.getStatus().isTerminal()) {
            composition.markProcessing();
            log.info("합성 처리 시작 - compositionId: {}", compositionId);
        }
    }

    /**
     * Records the heartbeat in memory — no DB connection acquired on normal ticks.
     * On the first heartbeat, delegates to CompositionStatusPromoter (a separate
     * Spring bean) so that @Transactional is honoured via AOP proxy.
     */
    public void updateHeartbeat(Long compositionId) {
        boolean isFirst = heartbeatStore.recordAndIsFirst(compositionId);
        if (isFirst) {
            statusPromoter.promoteQueuedToProcessing(compositionId);
        }
    }

    @Transactional(readOnly = true)
    public VideoCompositionStatusResponse getStatus(Long compositionId) {
        VideoComposition composition = videoCompositionRepository.findById(compositionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "VideoComposition을 찾을 수 없습니다. id=" + compositionId));
        return VideoCompositionStatusResponse.from(composition);
    }

    @Transactional
    public void markFailed(Long compositionId, String reason) {
        videoCompositionRepository.findById(compositionId).ifPresent(composition -> {
            if (!composition.getStatus().isTerminal()) {
                composition.markFailed(reason);
            }
        });
        heartbeatStore.remove(compositionId);
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
