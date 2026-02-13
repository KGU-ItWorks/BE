package com.streamly.streamly.domain.user.controller;

import com.streamly.streamly.domain.user.dto.UploaderRequestDto;
import com.streamly.streamly.domain.user.entity.UploaderRequest;
import com.streamly.streamly.domain.user.service.UploaderRequestService;
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

/**
 * 관리자 - 업로더 승급 신청 관리 API
 */
@Slf4j
@Tag(name = "Admin Uploader Request", description = "관리자 - 업로더 승급 신청 관리 API")
@RestController
@RequestMapping("/api/v1/admin/uploader-requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUploaderRequestController {
    
    private final UploaderRequestService requestService;
    
    /**
     * 모든 신청 조회
     */
    @Operation(summary = "모든 신청 조회", description = "모든 업로더 승급 신청을 조회합니다")
    @GetMapping
    public ResponseEntity<Page<UploaderRequestDto.Response>> getAllRequests(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        log.info("관리자 - 업로더 승급 신청 조회 - status: {}", status);
        
        Page<UploaderRequestDto.Response> requests;
        
        if (status != null && !status.isEmpty()) {
            UploaderRequest.RequestStatus requestStatus = UploaderRequest.RequestStatus.valueOf(status.toUpperCase());
            requests = requestService.getRequestsByStatus(requestStatus, pageable);
        } else {
            requests = requestService.getAllRequests(pageable);
        }
        
        return ResponseEntity.ok(requests);
    }
    
    /**
     * 대기 중인 신청 개수
     */
    @Operation(summary = "대기 중인 신청 개수", description = "처리 대기 중인 신청의 개수를 조회합니다")
    @GetMapping("/pending-count")
    public ResponseEntity<Long> getPendingCount() {
        long count = requestService.getPendingRequestCount();
        return ResponseEntity.ok(count);
    }
    
    /**
     * 신청 승인
     */
    @Operation(summary = "신청 승인", description = "업로더 승급 신청을 승인합니다")
    @PostMapping("/{requestId}/approve")
    public ResponseEntity<UploaderRequestDto.Response> approveRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long requestId
    ) {
        log.info("업로더 승급 신청 승인 - admin: {}, requestId: {}", userDetails.getUsername(), requestId);
        UploaderRequestDto.Response response = requestService.approveRequest(userDetails.getUsername(), requestId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 신청 거부
     */
    @Operation(summary = "신청 거부", description = "업로더 승급 신청을 거부합니다")
    @PostMapping("/{requestId}/reject")
    public ResponseEntity<UploaderRequestDto.Response> rejectRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long requestId,
            @RequestBody UploaderRequestDto.Reject dto
    ) {
        log.info("업로더 승급 신청 거부 - admin: {}, requestId: {}", userDetails.getUsername(), requestId);
        UploaderRequestDto.Response response = requestService.rejectRequest(userDetails.getUsername(), requestId, dto);
        return ResponseEntity.ok(response);
    }
}
