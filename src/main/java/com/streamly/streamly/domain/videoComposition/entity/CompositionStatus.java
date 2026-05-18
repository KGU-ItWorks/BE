package com.streamly.streamly.domain.videoComposition.entity;

public enum CompositionStatus {
    QUEUED,
    COMPLETED,
    FAILED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }
}
