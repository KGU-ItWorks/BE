package com.streamly.streamly.domain.videoComposition.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AiCompositionRequest(
        @NotBlank String startTime,
        Integer duration,
        @NotEmpty List<ClickPoint> points
) {

}
