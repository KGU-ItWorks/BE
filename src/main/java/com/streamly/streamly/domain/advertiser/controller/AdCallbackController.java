package com.streamly.streamly.domain.advertiser.controller;

import com.streamly.streamly.domain.advertiser.dto.AdVideoDto;
import com.streamly.streamly.domain.advertiser.service.AdVideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI 콜백 API", description = "AI 서버 → BE 누끼 처리 결과 콜백")
@Slf4j
@RestController
@RequestMapping("/api/v1/advertiser/callback")
@RequiredArgsConstructor
public class AdCallbackController {

    private final AdVideoService adVideoService;

    @Operation(summary = "누끼 처리 완료 콜백", description = "AI 서버에서 누끼 처리 완료 후 결과를 전달합니다.")
    @PostMapping("/nuki")
    public ResponseEntity<String> nukiCallback(@RequestBody AdVideoDto.NukiCallbackRequest request) {
        log.info("누끼 콜백 수신 - adVideoId: {}, success: {}", request.getAdVideoId(), request.isSuccess());
        adVideoService.handleNukiCallback(request);
        return ResponseEntity.ok("콜백 처리 완료");
    }
}
