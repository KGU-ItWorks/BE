package com.streamly.streamly.domain.advertiser.controller;

import com.streamly.streamly.domain.advertiser.dto.AdVideoDto;
import com.streamly.streamly.domain.advertiser.service.AdminAdVideoService;
import io.swagger.v3.oas.annotations.Operation;
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

@Tag(name = "관리자 광고 영상 API", description = "관리자 전용 광고 영상 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/ad-videos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAdVideoController {

    private final AdminAdVideoService adminAdVideoService;

    @Operation(summary = "전체 광고 영상 목록 조회", description = "모든 광고주의 광고 영상을 조회합니다.")
    @GetMapping
    public ResponseEntity<Page<AdVideoDto.Response>> getAllAdVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AdVideoDto.Response> response = adminAdVideoService.getAllAdVideos(status, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "광고 영상 강제 삭제")
    @DeleteMapping("/{adVideoId}")
    public ResponseEntity<String> forceDeleteAdVideo(@PathVariable Long adVideoId) {
        adminAdVideoService.forceDeleteAdVideo(adVideoId);
        log.info("관리자 광고 영상 강제 삭제 - adVideoId: {}", adVideoId);
        return ResponseEntity.ok("광고 영상이 삭제되었습니다.");
    }

    @Operation(summary = "광고 영상 대시보드 통계")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        return ResponseEntity.ok(adminAdVideoService.getDashboardStats());
    }
}
