package com.streamly.streamly.domain.videoComposition.entity;

import jakarta.validation.constraints.NotNull;

public record ClickPoint(@NotNull Double x, @NotNull Double y, @NotNull Integer label) {
}
