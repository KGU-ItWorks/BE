package com.streamly.streamly.domain.videoComposition.entity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AiCompositionRequest(
        @NotBlank String startTime,
        @NotNull @Positive Integer duration,
        @NotNull @Valid BoundingBox boundingBox
) {

}
