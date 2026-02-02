package com.streamly.streamly.domain.user.dto;

import com.streamly.streamly.domain.user.entity.Role;
import com.streamly.streamly.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String email;
    private String nickname;
    private Role role;
    private String provider;
    private LocalDateTime createdAt;

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.getProvider(),
                user.getCreatedAt()
        );
    }
}
