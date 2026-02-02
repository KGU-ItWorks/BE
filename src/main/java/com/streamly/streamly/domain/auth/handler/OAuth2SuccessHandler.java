package com.streamly.streamly.domain.auth.handler;

import com.streamly.streamly.domain.auth.entity.RefreshToken;
import com.streamly.streamly.domain.auth.jwt.JwtTokenProvider;
import com.streamly.streamly.domain.auth.repository.RefreshTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // email 속성 가져오기 (구글은 "email", 카카오는 중첩 구조)
        String email = oAuth2User.getAttribute("email");
        if (email == null) {
            // 카카오의 경우
            Map<String, Object> kakaoAccount = oAuth2User.getAttribute("kakao_account");
            if (kakaoAccount != null) {
                email = (String) kakaoAccount.get("email");
            }
        }

        String role = authentication.getAuthorities().iterator().next().getAuthority();

        // 1. 토큰 생성
        String accessToken = tokenProvider.createAccessToken(email, role);
        String refreshToken = tokenProvider.createRefreshToken();

        // 2. Redis 저장
        refreshTokenRepository.save(new RefreshToken(email, refreshToken));

        // 3. Access Token 쿠키 생성 (ResponseCookie 사용으로 보안 강화)
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", accessToken)
                .httpOnly(true)
                .secure(cookieSecure) // 환경변수로 관리
                .path("/")
                .maxAge(3600) // 1시간
                .sameSite("Lax") // CSRF 방지
                .build();
        response.addHeader("Set-Cookie", accessCookie.toString());

        // 4. Refresh Token 쿠키 생성
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(cookieSecure) // 환경변수로 관리
                .path("/")
                .maxAge(604800) // 7일
                .sameSite("Lax") // CSRF 방지
                .build();
        response.addHeader("Set-Cookie", refreshCookie.toString());

        // 5. 프론트엔드 메인 페이지로 이동
        getRedirectStrategy().sendRedirect(request, response, frontendUrl);
    }
}