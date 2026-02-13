package com.streamly.streamly.domain.user.entity;

import com.streamly.streamly.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity { // BaseEntity는 생성/수정일 자동 관리용
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    private String password; // 소셜 로그인 시 null 가능

    @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String provider; // "google", "kakao" 등
    private String providerId; // 소셜 서비스의 고유 ID

    @Column(nullable = false)
    private boolean active = true; // 계정 활성화 상태

    @Builder
    public User(String email, String password, String nickname, Role role, String provider, String providerId) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.role = role != null ? role : Role.ROLE_USER;
        this.provider = provider;
        this.providerId = providerId;
        this.active = true;
    }

    // 소셜 정보를 업데이트하기 위한 메서드
    public User update(String nickname) {
        this.nickname = nickname;
        return this;
    }

    // 권한 변경
    public void changeRole(Role newRole) {
        this.role = newRole;
    }

    // 활성화 상태 변경
    public void changeActiveStatus(boolean active) {
        this.active = active;
    }
}

