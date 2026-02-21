package com.streamly.streamly.domain.auth.dto;

import com.streamly.streamly.domain.user.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private String email;
    private String nickname;
    private Role role;
    private String message;
}
