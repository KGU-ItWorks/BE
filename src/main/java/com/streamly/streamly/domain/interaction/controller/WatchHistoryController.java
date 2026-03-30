package com.streamly.streamly.domain.interaction.controller;

import com.streamly.streamly.domain.interaction.dto.WatchHistoryResponse;
import com.streamly.streamly.domain.interaction.service.WatchHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "시청 기록 API", description = "시청 기록 저장, 조회, 삭제 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/interactions")
public class WatchHistoryController {

    private final WatchHistoryService watchHistoryService;

    @Operation(
            summary = "시청 기록 목록 조회",
            description = "현재 로그인한 사용자의 시청 기록을 최근 순으로 조회합니다."
    )
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/history")
    public ResponseEntity<Page<WatchHistoryResponse>> getWatchHistory(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {
        String email = authentication.getName();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "watchedAt"));
        return ResponseEntity.ok(watchHistoryService.getWatchHistory(email, pageable));
    }

    @Operation(
            summary = "시청 기록 저장",
            description = "영상 시청 시 현재 재생 위치를 저장합니다. 이미 기록이 있으면 업데이트합니다."
    )
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/history/{videoId}")
    public ResponseEntity<WatchHistoryResponse> recordWatch(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(description = "영상 ID") @PathVariable Long videoId,
            @Parameter(description = "마지막 재생 위치 (초)") @RequestParam(defaultValue = "0") Integer lastPositionSeconds) {
        String email = authentication.getName();
        return ResponseEntity.ok(watchHistoryService.recordWatch(email, videoId, lastPositionSeconds));
    }

    @Operation(
            summary = "특정 시청 기록 삭제",
            description = "특정 영상의 시청 기록을 삭제합니다."
    )
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/history/{videoId}")
    public ResponseEntity<Void> deleteWatchHistory(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(description = "영상 ID") @PathVariable Long videoId) {
        String email = authentication.getName();
        watchHistoryService.deleteWatchHistory(email, videoId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "시청 기록 전체 삭제",
            description = "현재 로그인한 사용자의 모든 시청 기록을 삭제합니다."
    )
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/history")
    public ResponseEntity<Void> clearAllWatchHistory(
            @Parameter(hidden = true) Authentication authentication) {
        String email = authentication.getName();
        watchHistoryService.clearAllWatchHistory(email);
        return ResponseEntity.noContent().build();
    }
}
