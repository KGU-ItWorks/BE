package com.streamly.streamly.domain.videoComposition.service;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store for tracking active compositions without DB writes.
 *
 * Two separate structures:
 *   pending — composition IDs that are QUEUED (registered at initialSave, removed on first heartbeat or terminal state)
 *   beats   — PROCESSING composition IDs with their last heartbeat timestamp
 *
 * hasAnyActive() lets the timeout scheduler skip both DB queries entirely
 * when no composition is in flight.
 *
 * Both structures are lost on BE restart, but:
 *   - The scheduler falls back to DB lastHeartbeatAt for the beats side.
 *   - The pending side falls back to DB QUEUED rows with old requestedAt.
 */
@Component
public class HeartbeatStore {

    private final ConcurrentHashMap<Long, Instant> beats   = new ConcurrentHashMap<>();
    private final Set<Long>                         pending = ConcurrentHashMap.newKeySet();

    // ── QUEUED tracking ──────────────────────────────────────────────────────

    /** Called when a new composition is persisted (QUEUED). */
    public void trackQueued(Long compositionId) {
        pending.add(compositionId);
    }

    /** Called when a composition leaves QUEUED for any reason. */
    public void untrackQueued(Long compositionId) {
        pending.remove(compositionId);
    }

    // ── PROCESSING heartbeat ─────────────────────────────────────────────────

    /**
     * Records the heartbeat and returns true if this is the first one seen
     * (i.e. the map had no previous entry for this compositionId).
     * Also removes the composition from the pending (QUEUED) set since it is now PROCESSING.
     */
    public boolean recordAndIsFirst(Long compositionId) {
        pending.remove(compositionId);
        return beats.put(compositionId, Instant.now()) == null;
    }

    /** Returns the last heartbeat time, or null if not seen since last restart. */
    public Instant getLastBeat(Long compositionId) {
        return beats.get(compositionId);
    }

    /** Called when a composition reaches a terminal state (COMPLETED / FAILED). */
    public void remove(Long compositionId) {
        beats.remove(compositionId);
        pending.remove(compositionId);
    }

    // ── Gate ─────────────────────────────────────────────────────────────────

    /**
     * Returns true if there is at least one composition that is QUEUED or PROCESSING
     * in memory. The timeout scheduler uses this to skip DB queries entirely when idle.
     */
    public boolean hasAnyActive() {
        return !pending.isEmpty() || !beats.isEmpty();
    }
}
