package com.streamly.streamly.domain.videoComposition.entity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AiCompositionRequest(
        @NotBlank String startTime,
        Integer duration,
        @NotNull @Valid BoundingBox boundingBox
) {

}
