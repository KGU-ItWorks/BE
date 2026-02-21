package com.streamly.streamly.global.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * CSRF 보호를 위한 커스텀 헤더 검증 필터
 * POST, PUT, DELETE 요청에 대해 X-Requested-With 헤더를 검증합니다.
 */
@Slf4j
@Component
public class CsrfHeaderFilter extends OncePerRequestFilter {

    private static final String CSRF_HEADER = "X-Requested-With";
    private static final String CSRF_HEADER_VALUE = "XMLHttpRequest";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String method = request.getMethod();
        String requestUri = request.getRequestURI();

        // Swagger UI 및 API 문서는 제외
        if (requestUri.startsWith("/v3/api-docs") || 
            requestUri.startsWith("/swagger-ui") ||
            requestUri.equals("/swagger-ui.html")) {
            filterChain.doFilter(request, response);
            return;
        }

        // OAuth2 엔드포인트는 제외 (브라우저 리다이렉트)
        if (requestUri.startsWith("/oauth2") || requestUri.startsWith("/login/oauth2")) {
            filterChain.doFilter(request, response);
            return;
        }

        // POST, PUT, DELETE 요청에 대해 커스텀 헤더 검증
        if ("POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method)) {
            String customHeader = request.getHeader(CSRF_HEADER);

            if (!CSRF_HEADER_VALUE.equals(customHeader)) {
                log.warn("CSRF 헤더 검증 실패 - URI: {}, Method: {}, Header: {}", 
                        requestUri, method, customHeader);
                
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(
                    "{\"error\":\"CSRF_PROTECTION\"," +
                    "\"message\":\"잘못된 요청입니다. 브라우저를 새로고침 후 다시 시도해주세요.\"}"
                );
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
