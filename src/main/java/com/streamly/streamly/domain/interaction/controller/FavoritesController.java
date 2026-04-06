package com.streamly.streamly.domain.interaction.controller;

import com.streamly.streamly.domain.interaction.dto.FavoritesResponse;
import com.streamly.streamly.domain.interaction.service.FavoritesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "찜 API", description = "영상 찜하기, 찜 목록 조회 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/interactions")
public class FavoritesController {
    private final FavoritesService favoritesService;

    @Operation(
            summary = "찜한 영상 목록 조회",
            description = "현재 로그인한 사용자의 찜한 영상 목록을 페이지네이션으로 조회합니다."
    )
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/favorites")
    public ResponseEntity<Page<FavoritesResponse>> getCurrentUsersFavorites(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size) {
        String email = authentication.getName();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<FavoritesResponse> favoriteVideos = favoritesService.getFavoriteVideos(email, pageable);
        return ResponseEntity.ok(favoriteVideos);
    }

    @Operation(
            summary = "찜 여부 확인",
            description = "특정 영상의 찜 여부를 반환합니다."
    )
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/favorites/{videoId}/status")
    public ResponseEntity<Map<String, Object>> checkFavorite(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(description = "영상 Id") @PathVariable Long videoId
    ) {
        String email = authentication.getName();
        boolean isFavorited = favoritesService.checkFavorite(email, videoId);
        return ResponseEntity.ok(Map.of("favorited", isFavorited));
    }

    @Operation(
            description = "토글 여부를 받아와 이미 찜 상태이면 삭제, 아니면 찜 목록에 추가합니다."
    )
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/favorites/{videoId}")
    public ResponseEntity<Map<String, Object>> toggleFavorite(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(description = "영상 Id") @PathVariable Long videoId
    ) {
        String email = authentication.getName();
        boolean isInFavor = favoritesService.toggleFavorite(email, videoId);
        return ResponseEntity.ok(Map.of(
                "favorited", isInFavor,
                "message", isInFavor ? "찜 목록에 추가되었습니다." : "찜 목록에서 제거되었습니다."
        ));
    }
}
