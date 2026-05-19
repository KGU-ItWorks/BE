package com.streamly.streamly.domain.videoComposition.service;

import com.streamly.streamly.domain.videoComposition.entity.CompositionStatus;
import com.streamly.streamly.domain.videoComposition.entity.VideoComposition;
import com.streamly.streamly.domain.videoComposition.repository.VideoCompositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompositionTimeoutScheduler {

    // How long a job can sit in the queue before we assume the AI server never got it.
    // Must be long enough for the AI server to finish any prior job before picking this one up.
    private static final int QUEUED_TIMEOUT_MINUTES   = 30;
    private static final int HEARTBEAT_TIMEOUT_SECONDS = 90;

    private final VideoCompositionRepository videoCompositionRepository;
    private final HeartbeatStore heartbeatStore;

    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void timeoutStaleCompositions() {
        if (!heartbeatStore.hasAnyActive()) return;
        expireStuckInQueue();
        expireSilentProcessing();
    }

    /** QUEUED > 5 min → AI server never picked the job up */
    private void expireStuckInQueue() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(QUEUED_TIMEOUT_MINUTES);
        List<VideoComposition> stale = videoCompositionRepository
                .findByStatusAndRequestedAtBefore(CompositionStatus.QUEUED, cutoff);

        if (stale.isEmpty()) return;
        stale.forEach(c -> {
            heartbeatStore.remove(c.getId());
            c.markFailed("AI 서버가 작업을 수신하지 못했습니다 (큐 대기 시간 초과)");
        });
        log.warn("[타임아웃] QUEUED 초과 {}건 FAILED 처리", stale.size());
    }

    /**
     * PROCESSING + no heartbeat > 90s → AI server crashed mid-job.
     *
     * Checks in-memory HeartbeatStore first (zero DB cost for live compositions).
     * Falls back to DB lastHeartbeatAt when the store has no entry
     * (e.g. after a BE restart while AI server was still processing).
     */
    private void expireSilentProcessing() {
        List<VideoComposition> processing = videoCompositionRepository
                .findByStatus(CompositionStatus.PROCESSING);

        Instant cutoff = Instant.now().minusSeconds(HEARTBEAT_TIMEOUT_SECONDS);

        List<VideoComposition> stale = processing.stream()
                .filter(c -> {
                    Instant lastBeat = heartbeatStore.getLastBeat(c.getId());
                    if (lastBeat != null) {
                        // In-memory path — no DB read needed
                        return lastBeat.isBefore(cutoff);
                    }
                    // Fallback: use DB lastHeartbeatAt (set by markProcessing or on restart recovery)
                    if (c.getLastHeartbeatAt() == null) return true;
                    Instant dbBeat = c.getLastHeartbeatAt().atZone(ZoneId.systemDefault()).toInstant();
                    return dbBeat.isBefore(cutoff);
                })
                .toList();

        if (stale.isEmpty()) return;
        stale.forEach(c -> {
            heartbeatStore.remove(c.getId());
            c.markFailed("AI 서버 응답 없음 (하트비트 시간 초과)");
        });
        log.warn("[타임아웃] PROCESSING 하트비트 초과 {}건 FAILED 처리", stale.size());
    }
}
