package com.streamly.streamly.domain.videoComposition.entity;

import jakarta.validation.constraints.NotNull;

/**
 * 객체 탐지용 바운딩 박스 — 프레임 크기에 무관한 정규화 좌표(0~1)로 표현한다.
 * (x, y)는 좌상단 모서리, (width, height)는 정규화된 너비/높이.
 */
public record BoundingBox(
        @NotNull Double x,
        @NotNull Double y,
        @NotNull Double width,
        @NotNull Double height
) {
}
