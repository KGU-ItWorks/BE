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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("인증 서비스 테스트")
class AuthServiceTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private SignupRequest signupRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 데이터 준비
        testUser = User.builder()
                .email("test@example.com")
                .password("encodedPassword")
                .nickname("테스트유저")
                .role(Role.ROLE_USER)
                .provider("local")
                .build();

        signupRequest = SignupRequest.builder()
                .email("test@example.com")
                .name("테스트유저")
                .password("password123!") // 정규식에 맞게 특수문자 포함 권장
                .build();

        loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("password123!")
                .build();
    }

    @Test
    @DisplayName("회원가입 성공")
    void signup_Success() {
        // given
        given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());
        given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");
        given(userRepository.save(any(User.class))).willReturn(testUser);

        // when
        authService.signup(signupRequest);

        // then
        then(userRepository).should(times(1)).findByEmail(signupRequest.getEmail());
        then(passwordEncoder).should(times(1)).encode(signupRequest.getPassword());
        then(userRepository).should(times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void signup_Fail_EmailAlreadyExists() {
        // given
        given(userRepository.findByEmail(anyString())).willReturn(Optional.of(testUser));

        // when & then
        assertThatThrownBy(() -> authService.signup(signupRequest))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("이미 사용 중인 이메일입니다.");

        then(userRepository).should(times(1)).findByEmail(signupRequest.getEmail());
        then(passwordEncoder).should(never()).encode(anyString());
        then(userRepository).should(never()).save(any(User.class));
    }

    @Test
    @DisplayName("로그인 성공")
    void login_Success() {
        // given
        given(userRepository.findByEmail(anyString())).willReturn(Optional.of(testUser));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);

        // when
        User result = authService.login(loginRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(testUser.getEmail());
        then(userRepository).should(times(1)).findByEmail(loginRequest.getEmail());
        then(passwordEncoder).should(times(1)).matches(loginRequest.getPassword(), testUser.getPassword());
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일")
    void login_Fail_UserNotFound() {
        // given
        given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("이메일 또는 비밀번호가 올바르지 않습니다.");

        then(userRepository).should(times(1)).findByEmail(loginRequest.getEmail());
        then(passwordEncoder).should(never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    void login_Fail_InvalidPassword() {
        // given
        given(userRepository.findByEmail(anyString())).willReturn(Optional.of(testUser));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("이메일 또는 비밀번호가 올바르지 않습니다.");

        then(userRepository).should(times(1)).findByEmail(loginRequest.getEmail());
        then(passwordEncoder).should(times(1)).matches(loginRequest.getPassword(), testUser.getPassword());
    }

    @Test
    @DisplayName("JWT 토큰 생성 성공")
    void generateTokens_Success() {
        // given
        String accessToken = "access.token.here";
        String refreshToken = "refresh.token.here";
        
        given(tokenProvider.createAccessToken(anyString(), anyString())).willReturn(accessToken);
        given(tokenProvider.createRefreshToken()).willReturn(refreshToken);
        given(refreshTokenRepository.save(any(RefreshToken.class))).willReturn(new RefreshToken(testUser.getEmail(), refreshToken));

        // when
        String[] tokens = authService.generateTokens(testUser);

        // then
        assertThat(tokens).hasSize(2);
        assertThat(tokens[0]).isEqualTo(accessToken);
        assertThat(tokens[1]).isEqualTo(refreshToken);
        
        then(tokenProvider).should(times(1)).createAccessToken(testUser.getEmail(), testUser.getRole().name());
        then(tokenProvider).should(times(1)).createRefreshToken();
        then(refreshTokenRepository).should(times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Access Token 갱신 성공")
    void refreshAccessToken_Success() {
        // given
        String refreshToken = "valid.refresh.token";
        String newAccessToken = "new.access.token";
        RefreshToken storedToken = new RefreshToken(testUser.getEmail(), refreshToken);

        given(tokenProvider.validateToken(anyString())).willReturn(true);
        given(refreshTokenRepository.findByRefreshToken(anyString())).willReturn(Optional.of(storedToken));
        given(userRepository.findByEmail(anyString())).willReturn(Optional.of(testUser));
        given(tokenProvider.createAccessToken(anyString(), anyString())).willReturn(newAccessToken);

        // when
        String result = authService.refreshAccessToken(refreshToken);

        // then
        assertThat(result).isEqualTo(newAccessToken);
        then(tokenProvider).should(times(1)).validateToken(refreshToken);
        then(refreshTokenRepository).should(times(1)).findByRefreshToken(refreshToken);
        then(userRepository).should(times(1)).findByEmail(testUser.getEmail());
        then(tokenProvider).should(times(1)).createAccessToken(testUser.getEmail(), testUser.getRole().name());
    }

    @Test
    @DisplayName("Access Token 갱신 실패 - 유효하지 않은 토큰")
    void refreshAccessToken_Fail_InvalidToken() {
        // given
        String invalidRefreshToken = "invalid.refresh.token";
        given(tokenProvider.validateToken(anyString())).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.refreshAccessToken(invalidRefreshToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("유효하지 않은 Refresh Token입니다.");

        then(tokenProvider).should(times(1)).validateToken(invalidRefreshToken);
        then(refreshTokenRepository).should(never()).findByRefreshToken(anyString());
    }

    @Test
    @DisplayName("Access Token 갱신 실패 - Refresh Token을 찾을 수 없음")
    void refreshAccessToken_Fail_TokenNotFound() {
        // given
        String refreshToken = "unknown.refresh.token";
        given(tokenProvider.validateToken(anyString())).willReturn(true);
        given(refreshTokenRepository.findByRefreshToken(anyString())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.refreshAccessToken(refreshToken))
                .isInstanceOf(TokenNotFoundException.class)
                .hasMessageContaining("Refresh Token을 찾을 수 없습니다.");

        then(tokenProvider).should(times(1)).validateToken(refreshToken);
        then(refreshTokenRepository).should(times(1)).findByRefreshToken(refreshToken);
        then(userRepository).should(never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("Access Token 갱신 실패 - 사용자를 찾을 수 없음")
    void refreshAccessToken_Fail_UserNotFound() {
        // given
        String refreshToken = "valid.refresh.token";
        RefreshToken storedToken = new RefreshToken("nonexistent@example.com", refreshToken);

        given(tokenProvider.validateToken(anyString())).willReturn(true);
        given(refreshTokenRepository.findByRefreshToken(anyString())).willReturn(Optional.of(storedToken));
        given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.refreshAccessToken(refreshToken))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다.");

        then(tokenProvider).should(times(1)).validateToken(refreshToken);
        then(refreshTokenRepository).should(times(1)).findByRefreshToken(refreshToken);
        then(userRepository).should(times(1)).findByEmail(storedToken.getEmail());
    }

    @Test
    @DisplayName("로그아웃 성공")
    void logout_Success() {
        // given
        String email = "test@example.com";
        willDoNothing().given(refreshTokenRepository).deleteById(anyString());

        // when
        authService.logout(email);

        // then
        then(refreshTokenRepository).should(times(1)).deleteById(email);
    }
}
