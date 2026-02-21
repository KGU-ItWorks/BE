package com.streamly.streamly.domain.video.controller;

import com.streamly.streamly.global.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * S3 연결 테스트용 컨트롤러 (개발/테스트용)
 * 프로덕션 배포 전 삭제 예정
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class S3TestController {

    private final S3Client s3Client;
    private final S3Service s3Service;

    /**
     * S3 연결 테스트
     * GET /api/v1/test/s3
     */
    @GetMapping("/s3")
    public ResponseEntity<Map<String, Object>> testS3Connection() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // S3 버킷 목록 조회
            ListBucketsResponse listBucketsResponse = s3Client.listBuckets();
            
            response.put("success", true);
            response.put("message", "S3 연결 성공!");
            response.put("bucketCount", listBucketsResponse.buckets().size());
            response.put("buckets", listBucketsResponse.buckets().stream()
                    .map(bucket -> bucket.name())
                    .toList());
            
            log.info("S3 연결 테스트 성공 - 버킷 수: {}", listBucketsResponse.buckets().size());
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "S3 연결 실패: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            
            log.error("S3 연결 테스트 실패", e);
        }
        
        return ResponseEntity.ok(response);
    }
}
