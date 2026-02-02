package com.streamly.streamly.global.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate Limiting 필터
 * 로그인 엔드포인트에 대한 요청 횟수를 제한하여 Brute Force 공격을 방어합니다.
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final ConcurrentHashMap<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> requestTimes = new ConcurrentHashMap<>();

    private static final int MAX_REQUESTS = 5; // 5번 시도
    private static final long TIME_WINDOW = 300000; // 5분 (300초)

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // 로그인 엔드포인트만 Rate Limiting 적용
        if ("/api/v1/auth/login".equals(path)) {
            String clientIp = getClientIp(request);

            if (isRateLimited(clientIp)) {
                log.warn("Rate Limit 초과 - IP: {}, Path: {}", clientIp, path);
                
                response.setStatus(429); // 429 Too Many Requests
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(
                    "{\"status\":429," +
                    "\"error\":\"TOO_MANY_REQUESTS\"," +
                    "\"message\":\"너무 많은 로그인 시도입니다. 5분 후 다시 시도해주세요.\"," +
                    "\"timestamp\":\"" + java.time.LocalDateTime.now() + "\"}"
                );
                return;
            }

            incrementRequestCount(clientIp);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRateLimited(String clientIp) {
        Long lastRequestTime = requestTimes.get(clientIp);

        if (lastRequestTime == null) {
            return false;
        }

        long currentTime = System.currentTimeMillis();

        // 시간 윈도우 초과 시 초기화
        if (currentTime - lastRequestTime > TIME_WINDOW) {
            requestCounts.remove(clientIp);
            requestTimes.remove(clientIp);
            return false;
        }

        AtomicInteger count = requestCounts.get(clientIp);
        return count != null && count.get() >= MAX_REQUESTS;
    }

    private void incrementRequestCount(String clientIp) {
        requestCounts.computeIfAbsent(clientIp, k -> new AtomicInteger(0)).incrementAndGet();
        requestTimes.put(clientIp, System.currentTimeMillis());
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // X-Forwarded-For는 여러 IP를 포함할 수 있으므로 첫 번째 IP 사용
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip;
    }
}
