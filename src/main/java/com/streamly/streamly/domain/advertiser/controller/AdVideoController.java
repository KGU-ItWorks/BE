package com.streamly.streamly.domain.advertiser.controller;

import com.streamly.streamly.domain.advertiser.dto.AdVideoDto;
import com.streamly.streamly.domain.advertiser.service.AdVideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Tag(name = "광고주 영상 API", description = "광고주 본인 영상 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/advertiser/videos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADVERTISER')")
public class AdVideoController {

    private final AdVideoService adVideoService;

    @Operation(summary = "광고 영상 업로드")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AdVideoDto.Response> uploadAdVideo(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart("request") AdVideoDto.UploadRequest request,
            @RequestPart("videoFile") MultipartFile videoFile,
            @RequestPart("adImageFile") MultipartFile adImageFile) throws IOException {

        log.info("광고 영상 업로드 요청 - user: {}", userDetails.getUsername());
        AdVideoDto.Response response = adVideoService.uploadAdVideo(
                userDetails.getUsername(), request, videoFile, adImageFile);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "내 광고 영상 목록 조회")
    @GetMapping
    public ResponseEntity<Page<AdVideoDto.Response>> getMyAdVideos(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AdVideoDto.Response> response = adVideoService.getMyAdVideos(
                userDetails.getUsername(), pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "내 광고 영상 상세 조회")
    @GetMapping("/{adVideoId}")
    public ResponseEntity<AdVideoDto.Response> getMyAdVideo(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long adVideoId) {

        AdVideoDto.Response response = adVideoService.getMyAdVideo(
                userDetails.getUsername(), adVideoId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "누끼 이미지 목록 조회")
    @GetMapping("/{adVideoId}/nuki")
    public ResponseEntity<AdVideoDto.NukiImagesResponse> getNukiImages(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long adVideoId) {

        AdVideoDto.NukiImagesResponse response = adVideoService.getNukiImages(
                userDetails.getUsername(), adVideoId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "내 광고 영상 삭제")
    @DeleteMapping("/{adVideoId}")
    public ResponseEntity<String> deleteMyAdVideo(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long adVideoId) {

        adVideoService.deleteMyAdVideo(userDetails.getUsername(), adVideoId);
        log.info("광고 영상 삭제 - user: {}, adVideoId: {}", userDetails.getUsername(), adVideoId);
        return ResponseEntity.ok("광고 영상이 삭제되었습니다.");
    }
}
