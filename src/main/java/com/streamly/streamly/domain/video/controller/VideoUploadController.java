package com.streamly.streamly.domain.video.controller;

import com.streamly.streamly.domain.user.entity.User;
import com.streamly.streamly.domain.video.dto.VideoUploadRequest;
import com.streamly.streamly.domain.video.dto.VideoUploadResponse;
import com.streamly.streamly.domain.video.service.VideoUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
@Tag(name = "Video Upload", description = "영상 업로드 API")
public class VideoUploadController {

    private final VideoUploadService videoUploadService;

    /**
     * 영상 업로드 API
     * 
     * @param request 영상 정보 (제목, 설명, 카테고리 등)
     * @param videoFile 영상 파일
     * @param thumbnailFile 썸네일 파일 (선택)
     * @param user 업로드하는 사용자
     * @return 업로드 결과
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "영상 업로드",
            description = "영상 파일과 썸네일을 업로드합니다. 업로드 후 자동으로 인코딩이 시작됩니다."
    )
    public ResponseEntity<VideoUploadResponse> uploadVideo(
            @Parameter(description = "영상 정보 (JSON)")
            @Valid @RequestPart("request") VideoUploadRequest request,
            
            @Parameter(description = "영상 파일 (MP4, AVI 등)")
            @RequestPart("videoFile") MultipartFile videoFile,
            
            @Parameter(description = "썸네일 이미지 (선택사항)")
            @RequestPart(value = "thumbnailFile", required = false) MultipartFile thumbnailFile,
            
            @AuthenticationPrincipal User user
    ) {
        try {
            log.info("Video upload request received from user: {}", user.getEmail());
            
            VideoUploadResponse response = videoUploadService.uploadVideo(
                    request,
                    videoFile,
                    thumbnailFile,
                    user
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid upload request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(VideoUploadResponse.builder()
                            .message("업로드 실패: " + e.getMessage())
                            .build());
                            
        } catch (IOException e) {
            log.error("File upload failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(VideoUploadResponse.builder()
                            .message("파일 업로드 중 오류가 발생했습니다.")
                            .build());
        }
    }
}
