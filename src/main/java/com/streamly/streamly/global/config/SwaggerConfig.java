package com.streamly.streamly.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI openAPI() {
        // JWT 인증 스키마 이름
        String jwtSchemeName = "JWT Authentication";
        String cookieSchemeName = "Cookie Authentication";

        // SecurityRequirement 설정
        SecurityRequirement jwtSecurityRequirement = new SecurityRequirement()
                .addList(jwtSchemeName);
        SecurityRequirement cookieSecurityRequirement = new SecurityRequirement()
                .addList(cookieSchemeName);

        // Components 설정 - SecurityScheme 등록
        Components components = new Components()
                // Bearer JWT 토큰 인증 (헤더)
                .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                        .name(jwtSchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .in(SecurityScheme.In.HEADER)
                        .description("Access Token을 입력하세요 (Bearer 접두사 제외)"))
                // Cookie 인증
                .addSecuritySchemes(cookieSchemeName, new SecurityScheme()
                        .name("accessToken")
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .description("브라우저 쿠키에 저장된 Access Token"));

        // 서버 정보
        Server localServer = new Server()
                .url("http://localhost:" + serverPort)
                .description("로컬 개발 서버");

        Server productionServer = new Server()
                .url("https://api.streamly.com")
                .description("프로덕션 서버");

        // Contact 정보
        Contact contact = new Contact()
                .name("Streamly 개발팀")
                .email("dev@streamly.com")
                .url("https://github.com/streamly");

        // API 정보
        Info info = new Info()
                .title("Streamly API Documentation")
                .description("""
                        ## Streamly 비디오 스트리밍 서비스 API
                        
                        ### 인증 방식
                        1. **JWT Bearer Token**: Authorization 헤더에 `Bearer {token}` 형식으로 전송
                        2. **Cookie**: 브라우저 쿠키에 `accessToken` 자동 포함
                        
                        ### 주요 기능
                        - 사용자 인증 (회원가입, 로그인, OAuth2)
                        - JWT 토큰 관리 (발급, 갱신, 로그아웃)
                        - 사용자 프로필 관리
                        - 비디오 업로드 및 스트리밍 (예정)
                        
                        ### 에러 코드
                        - **AUTH001~007**: 인증/권한 관련 오류
                        - **USER001~003**: 사용자 관련 오류
                        - **VIDEO001~004**: 비디오 관련 오류
                        - **COMMON001~005**: 공통 오류
                        
                        ### 토큰 사용 방법
                        1. `/api/v1/auth/login`으로 로그인
                        2. 응답으로 받은 쿠키(accessToken, refreshToken)는 자동으로 브라우저에 저장됨
                        3. 이후 요청 시 쿠키가 자동으로 포함되어 인증됨
                        4. Access Token 만료 시 `/api/v1/auth/refresh`로 갱신
                        """)
                .version("v1.0.0")
                .contact(contact);

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer, productionServer))
                .addSecurityItem(jwtSecurityRequirement)
                .addSecurityItem(cookieSecurityRequirement)
                .components(components);
    }
}
