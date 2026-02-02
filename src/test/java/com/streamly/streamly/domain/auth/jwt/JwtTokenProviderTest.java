package com.streamly.streamly.domain.auth.jwt;

import com.streamly.streamly.global.exception.CustomJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JWT 토큰 프로바이더 테스트")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    
    private final String SECRET_KEY = "dGhpcy1pcy1hLWV4YW1wbGUtc2VjcmV0LWtleS1mb3ItdnN0cmVhbS1wcm9qZWN0LW1vcmUtdGhhbi0zMi1jaGFycw==";
    private final long ACCESS_TOKEN_EXPIRATION = 3600000L; // 1시간
    private final long REFRESH_TOKEN_EXPIRATION = 604800000L; // 7일

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET_KEY, ACCESS_TOKEN_EXPIRATION, REFRESH_TOKEN_EXPIRATION);
    }

    @Test
    @DisplayName("Access Token 생성 성공")
    void createAccessToken_Success() {
        // given
        String email = "test@example.com";
        String role = "ROLE_USER";

        // when
        String token = jwtTokenProvider.createAccessToken(email, role);

        // then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT는 3부분으로 구성
    }

    @Test
    @DisplayName("Refresh Token 생성 성공")
    void createRefreshToken_Success() {
        // when
        String token = jwtTokenProvider.createRefreshToken();

        // then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("유효한 토큰 검증 성공")
    void validateToken_Success() {
        // given
        String email = "test@example.com";
        String role = "ROLE_USER";
        String token = jwtTokenProvider.createAccessToken(email, role);

        // when
        boolean isValid = jwtTokenProvider.validateToken(token);

        // then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("잘못된 형식의 토큰 검증 실패")
    void validateToken_Fail_MalformedToken() {
        // given
        String malformedToken = "invalid.token.format";

        // when
        boolean isValid = jwtTokenProvider.validateToken(malformedToken);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("토큰에서 인증 정보 조회 성공")
    void getAuthentication_Success() {
        // given
        String email = "test@example.com";
        String role = "ROLE_USER";
        String token = jwtTokenProvider.createAccessToken(email, role);

        // when
        Authentication authentication = jwtTokenProvider.getAuthentication(token);

        // then
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo(email);
        assertThat(authentication.getAuthorities()).isNotEmpty();
        assertThat(authentication.getAuthorities().toString()).contains(role);
    }

    @Test
    @DisplayName("토큰에서 이메일 추출 성공")
    void getEmailFromToken_Success() {
        // given
        String email = "test@example.com";
        String role = "ROLE_USER";
        String token = jwtTokenProvider.createAccessToken(email, role);

        // when
        String extractedEmail = jwtTokenProvider.getEmailFromToken(token);

        // then
        assertThat(extractedEmail).isEqualTo(email);
    }

    @Test
    @DisplayName("Subject가 없는 토큰에서 인증 정보 조회 실패")
    void getAuthentication_Fail_NoSubject() {
        // given - Refresh Token은 subject(email)이 없음
        String refreshToken = jwtTokenProvider.createRefreshToken();

        // when & then
        assertThatThrownBy(() -> jwtTokenProvider.getAuthentication(refreshToken))
                .isInstanceOf(CustomJwtException.class)
                .hasMessageContaining("이메일 정보가 없는 토큰입니다.");
    }

    @Test
    @DisplayName("권한 정보가 없는 토큰에서 인증 정보 조회 실패")
    void getAuthentication_Fail_NoAuthority() {
        // given - Refresh Token은 권한 정보(auth)가 없음
        String refreshToken = jwtTokenProvider.createRefreshToken();

        // when & then
        assertThatThrownBy(() -> jwtTokenProvider.getAuthentication(refreshToken))
                .isInstanceOf(CustomJwtException.class);
    }

    @Test
    @DisplayName("만료된 토큰 검증 실패")
    void validateToken_Fail_ExpiredToken() throws InterruptedException {
        // given - 매우 짧은 만료 시간을 가진 토큰 생성
        JwtTokenProvider shortLivedTokenProvider = new JwtTokenProvider(SECRET_KEY, 1L, 1L);
        String email = "test@example.com";
        String role = "ROLE_USER";
        String token = shortLivedTokenProvider.createAccessToken(email, role);

        // when - 토큰이 만료될 때까지 대기
        Thread.sleep(10);
        boolean isValid = shortLivedTokenProvider.validateToken(token);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("빈 토큰 검증 실패")
    void validateToken_Fail_EmptyToken() {
        // given
        String emptyToken = "";

        // when
        boolean isValid = jwtTokenProvider.validateToken(emptyToken);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("null 토큰 검증 실패")
    void validateToken_Fail_NullToken() {
        // given
        String nullToken = null;

        // when
        boolean isValid = jwtTokenProvider.validateToken(nullToken);

        // then
        assertThat(isValid).isFalse();
    }
}
