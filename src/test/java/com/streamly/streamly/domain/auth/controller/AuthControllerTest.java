package com.streamly.streamly.domain.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamly.streamly.domain.auth.dto.LoginRequest;
import com.streamly.streamly.domain.auth.dto.SignupRequest;
import com.streamly.streamly.domain.auth.service.AuthService;
import com.streamly.streamly.domain.user.entity.Role;
import com.streamly.streamly.domain.user.entity.User;
import com.streamly.streamly.global.exception.auth.InvalidCredentialsException;
import com.streamly.streamly.global.exception.user.EmailAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean; // 최신 버전용
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.servlet.http.Cookie; // Cookie 임포트

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("인증 컨트롤러 통합 테스트")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean // Deprecated된 @MockBean 대체
    private AuthService authService;

    private SignupRequest signupRequest;
    private LoginRequest loginRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Builder 패턴을 사용한 객체 생성 (비밀번호 정규식 충족)
        signupRequest = SignupRequest.builder()
                .email("test@example.com")
                .name("테스트유저")
                .password("password123!")
                .build();

        loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("password123!")
                .build();

        testUser = User.builder()
                .email("test@example.com")
                .password("encodedPassword")
                .nickname("테스트유저")
                .role(Role.ROLE_USER)
                .provider("local")
                .build();
    }

    @Test
    @DisplayName("회원가입 API 성공")
    void signup_Success() throws Exception {
        // given
        willDoNothing().given(authService).signup(any(SignupRequest.class));

        // when & then
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("회원가입이 완료되었습니다."));

        then(authService).should(times(1)).signup(any(SignupRequest.class));
    }

    @Test
    @DisplayName("회원가입 API 실패 - 이메일 중복")
    void signup_Fail_EmailAlreadyExists() throws Exception {
        // given
        willThrow(new EmailAlreadyExistsException()).given(authService).signup(any(SignupRequest.class));

        // when & then
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("USER002")); // $.code를 $.error로 수정

        then(authService).should(times(1)).signup(any(SignupRequest.class));
    }

    @Test
    @DisplayName("회원가입 API 실패 - 필수 필드 누락")
    void signup_Fail_ValidationError() throws Exception {
        // given
        SignupRequest invalidRequest = SignupRequest.builder()
                .email("")
                .password("")
                .name("")
                .build();

        // when & then
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        then(authService).should(never()).signup(any(SignupRequest.class));
    }

    @Test
    @DisplayName("로그인 API 성공")
    void login_Success() throws Exception {
        // given
        given(authService.login(any(LoginRequest.class))).willReturn(testUser);
        given(authService.generateTokens(any(User.class))).willReturn(new String[]{"accessToken", "refreshToken"});

        // when & then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(testUser.getEmail()))
                .andExpect(jsonPath("$.nickname").value(testUser.getNickname()))
                .andExpect(header().exists("Set-Cookie"));

        then(authService).should(times(1)).login(any(LoginRequest.class));
        then(authService).should(times(1)).generateTokens(any(User.class));
    }

    @Test
    @DisplayName("로그인 API 실패 - 잘못된 인증 정보")
    void login_Fail_InvalidCredentials() throws Exception {
        // given
        given(authService.login(any(LoginRequest.class)))
                .willThrow(new InvalidCredentialsException());

        // when & then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("AUTH001")); // $.code를 $.error로 수정

        then(authService).should(times(1)).login(any(LoginRequest.class));
        then(authService).should(never()).generateTokens(any(User.class));
    }

    @Test
    @DisplayName("토큰 갱신 API 성공")
    void refreshToken_Success() throws Exception {
        // given
        String newAccessToken = "new.access.token";
        given(authService.refreshAccessToken(anyString())).willReturn(newAccessToken);

        // when & then
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("refreshToken", "valid.refresh.token")))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(newAccessToken))
                .andExpect(header().exists("Set-Cookie"));

        then(authService).should(times(1)).refreshAccessToken(anyString());
    }

    @Test
    @DisplayName("토큰 갱신 API 실패 - Refresh Token 없음")
    void refreshToken_Fail_NoRefreshToken() throws Exception {
        // when & then
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Refresh Token이 없습니다."));

        then(authService).should(never()).refreshAccessToken(anyString());
    }
}