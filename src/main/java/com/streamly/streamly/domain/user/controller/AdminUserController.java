package com.streamly.streamly.domain.user.controller;

import com.streamly.streamly.domain.user.dto.UserResponse;
import com.streamly.streamly.domain.user.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자 - 사용자 관리 API
 */
@Slf4j
@Tag(name = "Admin User", description = "관리자 사용자 관리 API")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * 전체 사용자 목록 조회
     */
    @Operation(summary = "전체 사용자 목록", description = "모든 사용자를 조회합니다 (관리자 전용)")
    @GetMapping
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @PageableDefault(size = 100) Pageable pageable
    ) {
        log.info("관리자 - 전체 사용자 목록 조회");
        Page<UserResponse> users = adminUserService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }

    /**
     * 사용자 권한 변경
     */
    @Operation(summary = "사용자 권한 변경", description = "사용자의 role을 변경합니다")
    @PatchMapping("/{userId}/role")
    public ResponseEntity<UserResponse> changeUserRole(
            @PathVariable Long userId,
            @RequestParam String role
    ) {
        log.info("사용자 권한 변경 - userId: {}, newRole: {}", userId, role);
        UserResponse user = adminUserService.changeUserRole(userId, role);
        return ResponseEntity.ok(user);
    }

    /**
     * 사용자 활성화/비활성화
     */
    @Operation(summary = "사용자 상태 변경", description = "사용자를 활성화/비활성화합니다")
    @PatchMapping("/{userId}/status")
    public ResponseEntity<UserResponse> changeUserStatus(
            @PathVariable Long userId,
            @RequestParam boolean active
    ) {
        log.info("사용자 상태 변경 - userId: {}, active: {}", userId, active);
        UserResponse user = adminUserService.changeUserStatus(userId, active);
        return ResponseEntity.ok(user);
    }

    /**
     * 사용자 삭제
     */
    @Operation(summary = "사용자 삭제", description = "사용자를 완전히 삭제합니다")
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        log.info("사용자 삭제 - userId: {}", userId);
        adminUserService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
