package com.streamly.streamly.domain.video.controller;

import com.streamly.streamly.domain.video.entity.Video;
import com.streamly.streamly.domain.video.entity.VideoStatus;
import com.streamly.streamly.domain.video.repository.VideoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
@Tag(name = "Video Status", description = "영상 상태 조회 API")
public class VideoStatusController {

    private final VideoRepository videoRepository;

    /**
     * 영상 상태 조회
     */
    @GetMapping("/{videoId}/status")
    @Operation(summary = "영상 상태 조회", description = "영상의 현재 상태와 인코딩 진행률을 조회합니다.")
    public ResponseEntity<VideoStatusResponse> getVideoStatus(@PathVariable Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("영상을 찾을 수 없습니다."));

        VideoStatusResponse response = VideoStatusResponse.builder()
                .videoId(video.getId())
                .title(video.getTitle())
                .status(video.getStatus())
                .encodingProgress(video.getEncodingProgress())
                .durationSeconds(video.getDurationSeconds())
                .resolution(video.getResolution())
                .videoCodec(video.getVideoCodec())
                .audioCodec(video.getAudioCodec())
                .thumbnailUrl(video.getThumbnailUrl())
                .build();

        return ResponseEntity.ok(response);
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class VideoStatusResponse {
        private Long videoId;
        private String title;
        private VideoStatus status;
        private Integer encodingProgress;
        private Integer durationSeconds;
        private String resolution;
        private String videoCodec;
        private String audioCodec;
        private String thumbnailUrl;
    }
}
