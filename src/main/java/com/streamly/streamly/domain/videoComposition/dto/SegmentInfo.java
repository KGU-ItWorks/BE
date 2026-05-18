package com.streamly.streamly.domain.videoComposition.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SegmentInfo {
    private List<Integer> indices;
}
