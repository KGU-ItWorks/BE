package com.streamly.streamly.domain.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // 1. 쿠키에서 토큰 추출 (새로 추가한 로직)
            String token = resolveTokenFromCookie(request);

            // 2. 만약 쿠키에 없으면 기존처럼 Header에서 추출 (Swagger 등 테스트 용도)
            if (token == null) {
                token = resolveTokenFromHeader(request);
            }

            // 3. 토큰이 있고 유효한 경우에만 인증 처리
            if (token != null && !token.isEmpty() && tokenProvider.validateToken(token)) {
                Authentication authentication = tokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // 토큰 파싱 실패 시 디버그 레벨로 로그 남기고 계속 진행 (인증 없이 요청 처리)
            logger.debug("JWT 토큰 처리 중 오류 발생: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String resolveTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    String value = cookie.getValue();
                    // 빈 문자열이면 null 반환
                    return (value != null && !value.isEmpty()) ? value : null;
                }
            }
        }
        return null;
    }

    private String resolveTokenFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
