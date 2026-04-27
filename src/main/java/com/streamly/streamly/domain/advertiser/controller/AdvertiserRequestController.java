package com.streamly.streamly.domain.advertiser.controller;

import com.streamly.streamly.domain.advertiser.dto.AdvertiserRequestDto;
import com.streamly.streamly.domain.advertiser.service.AdvertiserRequestService;
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

@Tag(name = "광고주 신청 API", description = "광고주 권한 신청 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/advertiser-requests")
@RequiredArgsConstructor
public class AdvertiserRequestController {

    private final AdvertiserRequestService requestService;

    @Operation(summary = "광고주 신청")
    @PostMapping
    public ResponseEntity<AdvertiserRequestDto.Response> createRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody AdvertiserRequestDto.Create dto) {

        log.info("광고주 신청 - user: {}", userDetails.getUsername());
        return ResponseEntity.ok(requestService.createRequest(userDetails.getUsername(), dto));
    }

    @Operation(summary = "내 신청 내역 조회")
    @GetMapping("/my")
    public ResponseEntity<Page<AdvertiserRequestDto.Response>> getMyRequests(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(requestService.getMyRequests(userDetails.getUsername(), pageable));
    }
}
