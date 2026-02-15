package com.streamly.streamly.global.config;

import com.streamly.streamly.domain.auth.handler.OAuth2SuccessHandler;
import com.streamly.streamly.domain.auth.jwt.JwtAuthenticationFilter;
import com.streamly.streamly.domain.auth.service.CustomOAuth2UserService;
import com.streamly.streamly.global.filter.CsrfHeaderFilter;
import com.streamly.streamly.global.filter.RateLimitFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CsrfHeaderFilter csrfHeaderFilter;
    private final RateLimitFilter rateLimitFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 프로덕션 도메인 및 로컬 개발 환경 허용
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "https://streamlyai.store",           // ⚠️ 실제 도메인으로 변경
            "https://www.streamlyai.store",       // ⚠️ 실제 도메인으로 변경
            "http://localhost:3000"             // 로컬 개발용
        ));
        
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PATCH", "DELETE", "PUT", "OPTIONS"));
        configuration.setAllowCredentials(true);
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Set-Cookie", "Authorization", "Content-Type"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // HTTPS 강제 (프로덕션 환경에서 X-Forwarded-Proto 헤더 확인)
                .requiresChannel(channel -> channel
                    .requestMatchers(r -> r.getHeader("X-Forwarded-Proto") != null)
                    .requiresSecure()
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS 설정 적용
                .csrf(csrf -> csrf.disable()) // API 서버이므로 CSRF 비활성화
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 미사용
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login/**", "/oauth2/**").permitAll() // 인증 없이 접근 가능
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/v1/test/**").permitAll() // 테스트 API (개발용)
                        .requestMatchers("/api/v1/auth/signup", "/api/v1/auth/login").permitAll() // 회원가입, 로그인
                        .requestMatchers("/api/v1/auth/refresh").permitAll() // 토큰 갱신은 인증 없이 가능
                        .requestMatchers("/thumbnails/**", "/uploads/**").permitAll() // 정적 파일 (썸네일, 업로드)
                        .requestMatchers("/actuator/**").permitAll() // Actuator 전체 허용
                        .requestMatchers("/api/v1/users/me").authenticated() // 사용자 정보 조회는 인증 필요
                        .requestMatchers("/api/v1/videos/upload").hasAnyRole("UPLOADER", "ADMIN") // 업로드 권한 체크
                        .requestMatchers("/api/v1/videos").permitAll() // 영상 목록 조회는 인증 없이 가능
                        .requestMatchers("/api/v1/videos/**").permitAll() // 영상 상세 조회도 인증 없이 가능
                        .anyRequest().authenticated()

                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" + authException.getMessage() + "\"}");
                        })
                );

        // 필터 체인 순서: Rate Limit -> CSRF -> JWT -> UsernamePassword
        http.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(csrfHeaderFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
