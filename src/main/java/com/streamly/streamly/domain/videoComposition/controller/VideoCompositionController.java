package com.streamly.streamly.domain.videoComposition.controller;

import com.streamly.streamly.domain.video.dto.AiFetchResponse;
import com.streamly.streamly.domain.video.service.AiVideoService;
import com.streamly.streamly.domain.videoComposition.service.VideoCompositionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/video-compositions")
public class VideoCompositionController {

    private final AiVideoService aiVideoService;
    private final VideoCompositionService videoCompositionService;

    @Operation(
            summary = "AI 합성 요청",
            description = "AI 서버에 특정 영상 구간의 객체 탐지 및 합성을 요청합니다."
    )
    @PreAuthorize("hasAnyRole('UPLOADER', 'ADMIN')")
    @PostMapping("/{videoId}/ai-fetch")
    public ResponseEntity<Void> queueAiComposition(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(description = "영상 ID", required = true)
            @PathVariable Long videoId,
            @Parameter(description = "시작 시간 (HH:mm:ss, 기본값: 00:00:00)")
            @RequestParam(defaultValue = "00:00:00") String startTime,
            @Parameter(description = "구간 길이(초), 미입력 시 끝까지")
            @RequestParam(required = false) Integer duration,
            @Parameter(description = "객체 탐지 프롬프트", required = true)
            @RequestParam String objectPrompt) {

        String requesterEmail = authentication.getName();
        aiVideoService.requestAiComposition(requesterEmail, videoId, startTime, duration, objectPrompt);
        return ResponseEntity.accepted().build();
    }

    @Operation(
            summary = "AI 합성 완료 콜백",
            description = "AI 서버가 합성 완료 후 호출하는 내부 콜백 엔드포인트입니다."
    )
    @PostMapping("/{compositionId}/callback")
    public ResponseEntity<Void> aiCallback(
            @Parameter(description = "합성 요청 ID", required = true)
            @PathVariable Long compositionId,
            @RequestBody AiFetchResponse response) {

        log.info("AI 콜백 수신 - compositionId: {}, taskId: {}, success: {}",
                compositionId, response.getTaskId(), response.isSuccess());
        videoCompositionService.handleCallback(compositionId, response);
        return ResponseEntity.ok().build();
    }
}