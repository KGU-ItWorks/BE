package com.streamly.streamly.domain.user.controller;

import com.streamly.streamly.domain.user.dto.UploaderRequestDto;
import com.streamly.streamly.domain.user.service.UploaderRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * 업로더 승급 신청 API (사용자용)
 */
@Slf4j
@Tag(name = "Uploader Request", description = "업로더 승급 신청 API")
@RestController
@RequestMapping("/api/v1/uploader-requests")
@RequiredArgsConstructor
public class UploaderRequestController {
    
    private final UploaderRequestService requestService;
    
    /**
     * 업로더 승급 신청
     */
    @Operation(summary = "업로더 승급 신청", description = "일반 사용자가 업로더 권한을 신청합니다")
    @PostMapping
    public ResponseEntity<UploaderRequestDto.Response> createRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UploaderRequestDto.Create dto
    ) {
        log.info("업로더 승급 신청 - user: {}", userDetails.getUsername());
        UploaderRequestDto.Response response = requestService.createRequest(userDetails.getUsername(), dto);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 내 신청 내역 조회
     */
    @Operation(summary = "내 신청 내역", description = "내가 제출한 승급 신청 내역을 조회합니다")
    @GetMapping("/my")
    public ResponseEntity<Page<UploaderRequestDto.Response>> getMyRequests(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        log.info("내 신청 내역 조회 - user: {}", userDetails.getUsername());
        Page<UploaderRequestDto.Response> requests = requestService.getMyRequests(userDetails.getUsername(), pageable);
        return ResponseEntity.ok(requests);
    }
}
