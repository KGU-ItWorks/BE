package com.streamly.streamly.domain.advertiser.controller;

import com.streamly.streamly.domain.advertiser.dto.AdvertiserRequestDto;
import com.streamly.streamly.domain.advertiser.entity.AdvertiserRequest;
import com.streamly.streamly.domain.advertiser.service.AdvertiserRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "관리자 광고주 신청 API", description = "관리자 전용 광고주 신청 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/advertiser-requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAdvertiserRequestController {

    private final AdvertiserRequestService requestService;

    @Operation(summary = "전체 광고주 신청 목록 조회")
    @GetMapping
    public ResponseEntity<Page<AdvertiserRequestDto.Response>> getAllRequests(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {

        if (status != null) {
            AdvertiserRequest.RequestStatus requestStatus = AdvertiserRequest.RequestStatus.valueOf(status);
            return ResponseEntity.ok(requestService.getRequestsByStatus(requestStatus, pageable));
        }
        return ResponseEntity.ok(requestService.getAllRequests(pageable));
    }

    @Operation(summary = "광고주 신청 승인")
    @PostMapping("/{requestId}/approve")
    public ResponseEntity<AdvertiserRequestDto.Response> approveRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long requestId) {

        log.info("광고주 신청 승인 - admin: {}, requestId: {}", userDetails.getUsername(), requestId);
        return ResponseEntity.ok(requestService.approveRequest(userDetails.getUsername(), requestId));
    }

    @Operation(summary = "광고주 신청 거부")
    @PostMapping("/{requestId}/reject")
    public ResponseEntity<AdvertiserRequestDto.Response> rejectRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long requestId,
            @RequestBody AdvertiserRequestDto.Reject dto) {

        log.info("광고주 신청 거부 - admin: {}, requestId: {}", userDetails.getUsername(), requestId);
        return ResponseEntity.ok(requestService.rejectRequest(userDetails.getUsername(), requestId, dto));
    }

    @Operation(summary = "대기 중인 광고주 신청 수")
    @GetMapping("/pending-count")
    public ResponseEntity<Long> getPendingCount() {
        return ResponseEntity.ok(requestService.getPendingRequestCount());
    }
}
