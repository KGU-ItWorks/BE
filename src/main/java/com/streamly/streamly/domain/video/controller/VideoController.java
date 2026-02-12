package com.streamly.streamly.domain.video.controller;

import com.streamly.streamly.domain.video.dto.VideoResponse;
import com.streamly.streamly.domain.video.dto.VideoUploadRequest;
import com.streamly.streamly.domain.video.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "영상 API", description = "영상 업로드, 조회, 검색, 스트리밍 관련 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @Operation(
        summary = "영상 업로드",
        description = """
            영상 파일과 메타데이터를 업로드합니다.
            
            **권한:** ROLE_UPLOADER 이상 필요
            
            **파일 제한:**
            - 최대 크기: 5GB
            - 허용 형식: MP4, AVI, MOV, MKV 등 영상 파일
            
            **처리 과정:**
            1. 파일 업로드
            2. 메타데이터 저장
            3. 인코딩 작업 큐에 추가 (비동기)
            4. 인코딩 완료 후 S3 업로드
            5. CDN URL 생성
            """
    )
    @PreAuthorize("hasAnyRole('UPLOADER', 'ADMIN')")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VideoResponse> uploadVideo(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(description = "영상 제목", required = true)
            @RequestParam("title") String title,
            @Parameter(description = "영상 설명")
            @RequestParam(value = "description", required = false) String description,
            @Parameter(description = "카테고리 (예: 액션, 드라마, SF)")
            @RequestParam(value = "category", required = false) String category,
            @Parameter(description = "연령 등급 (예: 12+, 15+, 19+)")
            @RequestParam(value = "ageRating", required = false) String ageRating,
            @Parameter(description = "영상 파일", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "썸네일 이미지 파일 (선택)")
            @RequestParam(value = "thumbnailFile", required = false) MultipartFile thumbnailFile) {

        String email = authentication.getName();

        VideoUploadRequest request = VideoUploadRequest.builder()
                .title(title)
                .description(description)
                .category(category)
                .ageRating(ageRating)
                .build();

        VideoResponse response = videoService.uploadVideo(email, request, file, thumbnailFile);

        log.info("영상 업로드 요청 - User: {}, Title: {}, Size: {} bytes", 
                email, title, file.getSize());

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "공개된 영상 목록 조회",
        description = "승인되고 인코딩이 완료된 영상 목록을 페이지네이션으로 조회합니다."
    )
    @GetMapping
    public ResponseEntity<Page<VideoResponse>> getPublishedVideos(
            @Parameter(description = "페이지 번호 (0부터 시작)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "정렬 기준 (createdAt, viewCount 등)")
            @RequestParam(defaultValue = "publishedAt") String sortBy,
            @Parameter(description = "정렬 방향 (asc, desc)")
            @RequestParam(defaultValue = "desc") String direction) {

        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) 
            ? Sort.Direction.ASC 
            : Sort.Direction.DESC;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<VideoResponse> videos = videoService.getPublishedVideos(pageable);

        return ResponseEntity.ok(videos);
    }

    @Operation(
        summary = "영상 상세 조회",
        description = "영상 ID로 상세 정보를 조회합니다. 조회 시 조회수가 1 증가합니다."
    )
    @GetMapping("/{videoId}")
    public ResponseEntity<VideoResponse> getVideoById(
            @Parameter(description = "영상 ID", required = true)
            @PathVariable Long videoId) {
        
        VideoResponse video = videoService.getVideoById(videoId);
        return ResponseEntity.ok(video);
    }

    @Operation(
        summary = "카테고리별 영상 조회",
        description = "특정 카테고리의 영상을 조회합니다."
    )
    @GetMapping("/category/{category}")
    public ResponseEntity<Page<VideoResponse>> getVideosByCategory(
            @Parameter(description = "카테고리명", required = true)
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        Page<VideoResponse> videos = videoService.getVideosByCategory(category, pageable);

        return ResponseEntity.ok(videos);
    }

    @Operation(
        summary = "영상 검색",
        description = "제목 또는 설명에서 키워드를 검색합니다."
    )
    @GetMapping("/search")
    public ResponseEntity<Page<VideoResponse>> searchVideos(
            @Parameter(description = "검색 키워드", required = true)
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        Page<VideoResponse> videos = videoService.searchVideos(keyword, pageable);

        return ResponseEntity.ok(videos);
    }

    @Operation(
        summary = "내가 업로드한 영상 목록",
        description = "현재 로그인한 사용자가 업로드한 모든 영상을 조회합니다."
    )
    @PreAuthorize("hasAnyRole('UPLOADER', 'ADMIN')")
    @GetMapping("/my")
    public ResponseEntity<Page<VideoResponse>> getMyVideos(
            @Parameter(hidden = true) Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        String email = authentication.getName();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<VideoResponse> videos = videoService.getMyVideos(email, pageable);

        return ResponseEntity.ok(videos);
    }

    @Operation(
        summary = "조회수 상위 영상",
        description = "조회수가 높은 순서대로 영상을 조회합니다."
    )
    @GetMapping("/top-viewed")
    public ResponseEntity<Page<VideoResponse>> getTopViewedVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<VideoResponse> videos = videoService.getTopViewedVideos(pageable);

        return ResponseEntity.ok(videos);
    }

    @Operation(
        summary = "최신 영상",
        description = "최근 업로드된 영상을 조회합니다."
    )
    @GetMapping("/recent")
    public ResponseEntity<Page<VideoResponse>> getRecentVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<VideoResponse> videos = videoService.getRecentVideos(pageable);

        return ResponseEntity.ok(videos);
    }

    @Operation(
        summary = "영상 인코딩 상태 조회",
        description = "영상의 인코딩 진행 상태를 조회합니다. 인코딩 진행률과 메타데이터를 확인할 수 있습니다."
    )
    @GetMapping("/{videoId}/status")
    public ResponseEntity<VideoResponse> getVideoStatus(
            @Parameter(description = "영상 ID", required = true)
            @PathVariable Long videoId) {
        
        VideoResponse video = videoService.getVideoById(videoId);
        return ResponseEntity.ok(video);
    }

    @Operation(
        summary = "영상 정보 수정",
        description = "영상의 제목, 설명, 카테고리 등 메타정보를 수정합니다. 업로더 본인 또는 관리자만 수정 가능합니다."
    )
    @PreAuthorize("hasAnyRole('UPLOADER', 'ADMIN')")
    @PutMapping("/{videoId}")
    public ResponseEntity<VideoResponse> updateVideo(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(description = "영상 ID", required = true)
            @PathVariable Long videoId,
            @Valid @RequestBody com.streamly.streamly.domain.video.dto.VideoUpdateRequest request) {
        
        String email = authentication.getName();
        VideoResponse response = videoService.updateVideo(email, videoId, request);

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "영상 삭제",
        description = "영상을 삭제합니다. 업로더 본인 또는 관리자만 삭제 가능합니다."
    )
    @PreAuthorize("hasAnyRole('UPLOADER', 'ADMIN')")
    @DeleteMapping("/{videoId}")
    public ResponseEntity<String> deleteVideo(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(description = "영상 ID", required = true)
            @PathVariable Long videoId) {
        
        String email = authentication.getName();
        videoService.deleteVideo(email, videoId);

        return ResponseEntity.ok("영상이 삭제되었습니다.");
    }
}
