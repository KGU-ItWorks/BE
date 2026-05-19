package com.streamly.streamly.domain.videoComposition.service;

import com.streamly.streamly.domain.videoComposition.entity.CompositionStatus;
import com.streamly.streamly.domain.videoComposition.repository.VideoCompositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Extracted into its own Spring bean so that @Transactional is applied via AOP proxy.
 * Called only on the first heartbeat per composition — all subsequent heartbeats
 * are purely in-memory and never touch this class.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompositionStatusPromoter {

    private final VideoCompositionRepository videoCompositionRepository;

    @Transactional
    public void promoteQueuedToProcessing(Long compositionId) {
        videoCompositionRepository.findById(compositionId)
                .filter(c -> c.getStatus() == CompositionStatus.QUEUED)
                .ifPresent(c -> {
                    c.markProcessing();
                    log.info("첫 하트비트로 PROCESSING 전환 - compositionId: {}", compositionId);
                });
    }
}
