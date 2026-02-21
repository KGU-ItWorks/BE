package com.streamly.streamly.domain.user.service;

import com.streamly.streamly.domain.user.dto.UserResponse;
import com.streamly.streamly.domain.user.dto.UserUpdateRequest;
import com.streamly.streamly.domain.user.entity.User;
import com.streamly.streamly.domain.user.repository.UserRepository;
import com.streamly.streamly.global.exception.user.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateProfile(String email, UserUpdateRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        user.update(request.getNickname());
        return UserResponse.from(user);
    }
}
