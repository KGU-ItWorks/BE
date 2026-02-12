package com.streamly.streamly.domain.video.controller;

import com.streamly.streamly.domain.video.dto.VideoResponse;
import com.streamly.streamly.domain.video.service.AdminVideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "관리자 영상 API", description = "관리자 전용 영상 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/videos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminVideoController {

    private final AdminVideoService adminVideoService;

    @Operation(
        summary = "전체 영상 목록 조회",
        description = "관리자가 모든 영상을 상태와 관계없이 조회합니다."
    )
    @GetMapping
    public ResponseEntity<Page<VideoResponse>> getAllVideos(
            @Parameter(description = "페이지 번호 (0부터 시작)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "영상 상태 필터 (UPLOADING, UPLOADED, ENCODING, COMPLETED, FAILED, DELETED)")
            @RequestParam(required = false) String status,
            @Parameter(description = "승인 상태 필터 (PENDING, APPROVED, REJECTED)")
            @RequestParam(required = false) String approvalStatus) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<VideoResponse> videos = adminVideoService.getAllVideos(status, approvalStatus, pageable);

        return ResponseEntity.ok(videos);
    }

    @Operation(
        summary = "영상 승인",
        description = "대기 중인 영상을 승인하여 공개합니다."
    )
    @PostMapping("/{videoId}/approve")
    public ResponseEntity<VideoResponse> approveVideo(
            @Parameter(description = "영상 ID", required = true)
            @PathVariable Long videoId) {

        VideoResponse response = adminVideoService.approveVideo(videoId);
        log.info("영상 승인 - ID: {}", videoId);

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "영상 거부",
        description = "대기 중인 영상을 거부합니다."
    )
    @PostMapping("/{videoId}/reject")
    public ResponseEntity<VideoResponse> rejectVideo(
            @Parameter(description = "영상 ID", required = true)
            @PathVariable Long videoId,
            @Parameter(description = "거부 사유", required = true)
            @RequestParam String reason) {

        VideoResponse response = adminVideoService.rejectVideo(videoId, reason);
        log.info("영상 거부 - ID: {}, Reason: {}", videoId, reason);

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "영상 강제 삭제",
        description = "관리자가 모든 영상을 강제 삭제할 수 있습니다."
    )
    @DeleteMapping("/{videoId}")
    public ResponseEntity<String> forceDeleteVideo(
            @Parameter(description = "영상 ID", required = true)
            @PathVariable Long videoId) {

        adminVideoService.forceDeleteVideo(videoId);
        log.info("영상 강제 삭제 - ID: {}", videoId);

        return ResponseEntity.ok("영상이 삭제되었습니다.");
    }

    @Operation(
        summary = "대시보드 통계",
        description = "영상 관련 전체 통계를 조회합니다."
    )
    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = adminVideoService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }

    @Operation(
        summary = "승인 대기 영상 수",
        description = "승인이 필요한 영상 개수를 조회합니다."
    )
    @GetMapping("/pending-count")
    public ResponseEntity<Long> getPendingVideosCount() {
        Long count = adminVideoService.getPendingVideosCount();
        return ResponseEntity.ok(count);
    }
}
