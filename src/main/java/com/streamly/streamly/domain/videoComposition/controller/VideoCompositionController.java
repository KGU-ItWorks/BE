package com.streamly.streamly.domain.videoComposition.controller;

import com.streamly.streamly.domain.advertiser.dto.AdVideoDto;
import com.streamly.streamly.domain.advertiser.service.AdVideoService;
import com.streamly.streamly.domain.user.repository.UserRepository;
import com.streamly.streamly.domain.video.dto.AiFetchResponse;
import com.streamly.streamly.domain.video.service.AiVideoService;
import com.streamly.streamly.domain.videoComposition.dto.AdInfoDto;
import com.streamly.streamly.domain.videoComposition.dto.VideoCompositionDto;
import com.streamly.streamly.domain.videoComposition.dto.VideoCompositionStatusResponse;
import com.streamly.streamly.domain.videoComposition.entity.VideoComposition;
import com.streamly.streamly.domain.videoComposition.service.VideoCompositionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/video-compositions")
public class VideoCompositionController {

    private final AiVideoService aiVideoService;
    private final VideoCompositionService videoCompositionService;
    private final AdVideoService adVideoService;


    @GetMapping("/{videoId}/ad-info")
    public ResponseEntity<AdInfoDto> getAdInfo(
            @PathVariable Long videoId
    ){
        VideoComposition videoComposition = videoCompositionService.getCurrentVideoComposition(videoId)
                .orElse(null);

        if (videoComposition == null) {
            return ResponseEntity.ok(AdInfoDto.noAd());
        }
        AdVideoDto.Response adVideoById = adVideoService.getAdVideoById(videoComposition.getAdVideoId());
        String nukiImageUrl = adVideoService.getFirstNukiImageUrl(videoComposition.getAdVideoId());

        return ResponseEntity.ok(AdInfoDto.from(
                true,
                videoComposition.getAdVideoId(),
                adVideoById.getAdvertiserNickname(),
                adVideoById.getDescription(),
                nukiImageUrl
        ));
    }

    // -------------------------------------------------------------------------
    // Uploader — request AI composition
    // -------------------------------------------------------------------------

    @Operation(
            summary = "AI 합성 요청",
            description = "AI 서버에 특정 영상 구간의 객체 탐지 및 합성을 요청합니다."
    )
    @PreAuthorize("hasAnyRole('UPLOADER', 'ADMIN')")
    @PostMapping("/{videoId}/ai-fetch")
    public ResponseEntity<Map<String, Long>> queueAiComposition(
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
        Long compositionId = aiVideoService.requestAiComposition(requesterEmail, videoId, startTime, duration, objectPrompt);
        return ResponseEntity.accepted().body(Map.of("compositionId", compositionId));
    }

    // -------------------------------------------------------------------------
    // Advertiser — pending list, approve, reject
    // -------------------------------------------------------------------------

    @Operation(
            summary = "광고주 미승인 합성 영상 목록",
            description = "내 광고 영상이 사용된 합성 결과 중 PENDING 상태인 목록을 반환합니다. hlsPath로 미리보기 재생 가능."
    )
    @PreAuthorize("hasAnyRole('ADVERTISER')")
    @GetMapping("/pending")
    public ResponseEntity<Page<VideoCompositionDto>> getPendingCompositions(
            @Parameter(hidden = true) Authentication authentication,
            Pageable pageable) {
        return ResponseEntity.ok(
                videoCompositionService.getPendingComposedVideosByAdvertiser(authentication.getName(), pageable));
    }

    @Operation(
            summary = "합성 영상 승인",
            description = "광고주가 합성 결과를 승인합니다. 승인 즉시 published_at이 기록되고 일반 사용자에게 노출됩니다."
    )
    @PreAuthorize("hasAnyRole('ADVERTISER')")
    @PostMapping("/{compositionId}/approve")
    public ResponseEntity<Void> approve(
            @Parameter(description = "합성 요청 ID", required = true)
            @PathVariable Long compositionId,
            @Parameter(hidden = true) Authentication authentication) {
        videoCompositionService.approve(compositionId, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "합성 영상 거절",
            description = "광고주가 합성 결과를 거절합니다. 거절된 영상은 일반 사용자에게 노출되지 않습니다."
    )
    @PreAuthorize("hasAnyRole('ADVERTISER')")
    @PostMapping("/{compositionId}/reject")
    public ResponseEntity<Void> reject(
            @Parameter(description = "합성 요청 ID", required = true)
            @PathVariable Long compositionId,
            @RequestBody VideoCompositionDto.RejectRequest request,
            @Parameter(hidden = true) Authentication authentication) {
        videoCompositionService.reject(compositionId, request.getReason(), authentication.getName());
        return ResponseEntity.ok().build();
    }

    // -------------------------------------------------------------------------
    // AI pipeline — internal callbacks
    // -------------------------------------------------------------------------

    @Operation(summary = "합성 상태 조회", description = "compositionId로 AI 합성 진행 상태를 조회합니다.")
    @GetMapping("/{compositionId}/status")
    public ResponseEntity<VideoCompositionStatusResponse> getStatus(
            @Parameter(description = "합성 요청 ID", required = true)
            @PathVariable Long compositionId) {
        return ResponseEntity.ok(videoCompositionService.getStatus(compositionId));
    }

    @Operation(summary = "AI 합성 시작 알림", description = "AI 서버가 작업을 시작할 때 호출하는 내부 엔드포인트입니다.")
    @PostMapping("/{compositionId}/start")
    public ResponseEntity<Void> aiStart(
            @Parameter(description = "합성 요청 ID", required = true)
            @PathVariable Long compositionId) {
        videoCompositionService.markProcessing(compositionId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "AI 하트비트", description = "AI 서버가 처리 중 주기적으로 호출하는 생존 신호 엔드포인트입니다.")
    @PostMapping("/{compositionId}/heartbeat")
    public ResponseEntity<Void> aiHeartbeat(
            @Parameter(description = "합성 요청 ID", required = true)
            @PathVariable Long compositionId) {
        videoCompositionService.updateHeartbeat(compositionId);
        return ResponseEntity.ok().build();
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

        log.info("AI 콜백 수신 - compositionId: {}, taskId: {}, success: {}, adVideoId: {}",
                compositionId, response.getTaskId(), response.isSuccess(), response.getAdVideoId());
        videoCompositionService.handleCallback(compositionId, response);
        return ResponseEntity.ok().build();
    }
}
