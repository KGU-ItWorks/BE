package com.streamly.streamly.domain.auth.service;

import com.streamly.streamly.domain.auth.dto.LoginRequest;
import com.streamly.streamly.domain.auth.dto.SignupRequest;
import com.streamly.streamly.domain.auth.entity.RefreshToken;
import com.streamly.streamly.domain.auth.jwt.JwtTokenProvider;
import com.streamly.streamly.domain.auth.repository.RefreshTokenRepository;
import com.streamly.streamly.domain.user.entity.Role;
import com.streamly.streamly.domain.user.entity.User;
import com.streamly.streamly.domain.user.repository.UserRepository;
import com.streamly.streamly.global.exception.auth.InvalidCredentialsException;
import com.streamly.streamly.global.exception.auth.InvalidTokenException;
import com.streamly.streamly.global.exception.auth.TokenNotFoundException;
import com.streamly.streamly.global.exception.user.EmailAlreadyExistsException;
import com.streamly.streamly.global.exception.user.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public String[] refreshAccessToken(String refreshToken) {
        // 1. Refresh Token 유효성 검증
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException("유효하지 않은 Refresh Token입니다.");
        }

        // 2. Redis에서 Refresh Token 조회
        RefreshToken storedToken = refreshTokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new TokenNotFoundException("Refresh Token을 찾을 수 없습니다."));

        // 3. 사용자 정보 조회
        User user = userRepository.findByEmail(storedToken.getEmail())
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        // 4. 새로운 Access Token 생성
        String newAccessToken = tokenProvider.createAccessToken(user.getEmail(), user.getRole().name());
        
        // 5. 새로운 Refresh Token 생성 (Rotation)
        String newRefreshToken = tokenProvider.createRefreshToken();
        
        // 6. 기존 Refresh Token 삭제
        refreshTokenRepository.deleteById(storedToken.getEmail());
        
        // 7. 새 Refresh Token 저장
        refreshTokenRepository.save(new RefreshToken(user.getEmail(), newRefreshToken));
        
        // 8. 두 토큰 모두 반환
        return new String[]{newAccessToken, newRefreshToken};
    }

    @Transactional
    public void logout(String email) {
        // Redis에서 Refresh Token 삭제
        refreshTokenRepository.deleteById(email);
    }

    // 자체 회원가입
    @Transactional
    public void signup(SignupRequest request) {
        // 1. 이메일 중복 체크
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException("이미 사용 중인 이메일입니다.");
        }

        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 3. 사용자 생성
        User user = User.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .nickname(request.getName())
                .role(Role.ROLE_USER)
                .provider("local") // 자체 회원가입
                .providerId(null)
                .build();

        userRepository.save(user);
    }

    // 자체 로그인 + JWT 토큰 생성
    @Transactional
    public User login(LoginRequest request) {
        // 1. 이메일로 사용자 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다."));

        // 2. 비밀번호 확인
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return user;
    }

    // JWT 토큰 생성 및 Redis 저장
    @Transactional
    public String[] generateTokens(User user) {
        String accessToken = tokenProvider.createAccessToken(user.getEmail(), user.getRole().name());
        String refreshToken = tokenProvider.createRefreshToken();

        // Redis에 Refresh Token 저장
        refreshTokenRepository.save(new RefreshToken(user.getEmail(), refreshToken));

        return new String[]{accessToken, refreshToken};
    }
}
