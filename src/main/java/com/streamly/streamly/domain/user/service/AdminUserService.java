package com.streamly.streamly.domain.user.service;

import com.streamly.streamly.domain.user.dto.UserResponse;
import com.streamly.streamly.domain.user.entity.Role;
import com.streamly.streamly.domain.user.entity.User;
import com.streamly.streamly.domain.user.repository.UserRepository;
import com.streamly.streamly.global.exception.user.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 - 사용자 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    /**
     * 전체 사용자 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(UserResponse::from);
    }

    /**
     * 사용자 권한 변경
     */
    @Transactional
    public UserResponse changeUserRole(Long userId, String roleStr) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        // Role enum으로 변환
        Role newRole;
        try {
            // "ROLE_ADMIN" 또는 "ADMIN" 둘 다 처리
            String normalizedRole = roleStr.startsWith("ROLE_") ? roleStr : "ROLE_" + roleStr;
            newRole = Role.valueOf(normalizedRole);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 권한입니다: " + roleStr);
        }

        user.changeRole(newRole);
        User savedUser = userRepository.save(user);

        log.info("사용자 권한 변경 완료 - userId: {}, newRole: {}", userId, newRole);

        return UserResponse.from(savedUser);
    }

    /**
     * 사용자 활성화/비활성화
     */
    @Transactional
    public UserResponse changeUserStatus(Long userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        user.changeActiveStatus(active);
        User savedUser = userRepository.save(user);

        log.info("사용자 상태 변경 완료 - userId: {}, active: {}", userId, active);

        return UserResponse.from(savedUser);
    }

    /**
     * 사용자 삭제
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        // TODO: 사용자가 업로드한 영상도 함께 삭제할지 결정

        userRepository.delete(user);

        log.info("사용자 삭제 완료 - userId: {}", userId);
    }
}
