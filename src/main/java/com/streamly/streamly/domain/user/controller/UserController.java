package com.streamly.streamly.domain.user.controller;

import com.streamly.streamly.domain.user.dto.UserResponse;
import com.streamly.streamly.domain.user.dto.UserUpdateRequest;
import com.streamly.streamly.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "사용자 API", description = "사용자 정보 조회 및 수정 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "현재 로그인한 사용자 정보 조회", description = "JWT 토큰으로 인증된 사용자의 정보를 반환합니다.")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        String email = authentication.getName();
        UserResponse userResponse = userService.getCurrentUser(email);
        return ResponseEntity.ok(userResponse);
    }

    @Operation(summary = "사용자 프로필 수정", description = "닉네임 등 사용자 프로필을 수정합니다.")
    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UserUpdateRequest request) {

        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        String email = authentication.getName();
        UserResponse userResponse = userService.updateProfile(email, request);
        return ResponseEntity.ok(userResponse);
    }
}
