package com.streamly.streamly.domain.auth.controller;

import com.streamly.streamly.domain.auth.dto.LoginRequest;
import com.streamly.streamly.domain.auth.dto.SignupRequest;
import com.streamly.streamly.domain.auth.dto.TokenResponse;
import com.streamly.streamly.domain.auth.service.AuthService;
import com.streamly.streamly.domain.user.dto.UserResponse;
import com.streamly.streamly.domain.user.entity.User;
import com.streamly.streamly.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증 API", description = "회원가입, 로그인, 로그아웃, 토큰 갱신 등 사용자 인증 관련 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${cookie.secure:false}")
    private boolean cookieSecure;

    @Operation(
            summary = "자체 회원가입",
            description = """
                    이메일, 이름, 비밀번호로 회원가입합니다.
                    
                    **필수 정보:**
                    - email: 유효한 이메일 형식
                    - password: 8자 이상
                    - name: 사용자 이름
                    
                    **제약사항:**
                    - 이메일은 중복될 수 없습니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "회원가입 성공",
                    content = @Content(
                            mediaType = "text/plain",
                            examples = @ExampleObject(value = "회원가입이 완료되었습니다.")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (유효성 검증 실패)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "status": 400,
                                      "code": "VALIDATION_ERROR",
                                      "message": "이메일 형식이 올바르지 않습니다."
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "이메일 중복",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "status": 409,
                                      "code": "USER002",
                                      "message": "이미 사용 중인 이메일입니다."
                                    }
                                    """)
                    )
            )
    })
    @PostMapping("/signup")
    public ResponseEntity<String> signup(
            @Parameter(description = "회원가입 요청 정보", required = true)
            @Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.ok("회원가입이 완료되었습니다.");
    }

    @Operation(
            summary = "자체 로그인",
            description = """
                    이메일, 비밀번호로 로그인하고 JWT 토큰을 쿠키에 설정합니다.
                    
                    **응답:**
                    - 사용자 정보 (JSON)
                    - Set-Cookie 헤더 (accessToken, refreshToken)
                    
                    **쿠키 정보:**
                    - accessToken: 1시간 유효
                    - refreshToken: 7일 유효
                    - HttpOnly, SameSite=Lax 설정
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "id": 1,
                                      "email": "test@example.com",
                                      "nickname": "테스트유저",
                                      "role": "ROLE_USER",
                                      "provider": "local"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 (이메일 또는 비밀번호 오류)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "status": 401,
                                      "code": "AUTH001",
                                      "message": "이메일 또는 비밀번호가 올바르지 않습니다."
                                    }
                                    """)
                    )
            )
    })
    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(
            @Parameter(description = "로그인 요청 정보", required = true)
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        // 1. 로그인 (이메일, 비밀번호 검증)
        User user = authService.login(request);

        // 2. JWT 토큰 생성
        String[] tokens = authService.generateTokens(user);
        String accessToken = tokens[0];
        String refreshToken = tokens[1];

        // 3. Access Token 쿠키 설정
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", accessToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(1800) // 30분
                .sameSite("Lax")
                .build();
        response.addHeader("Set-Cookie", accessCookie.toString());

        // 4. Refresh Token 쿠키 설정
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(604800) // 7일
                .sameSite("Lax")
                .build();
        response.addHeader("Set-Cookie", refreshCookie.toString());

        // 5. 응답 (사용자 전체 정보 반환)
        UserResponse userResponse = UserResponse.from(user);
        return ResponseEntity.ok(userResponse);
    }

    @Operation(
            summary = "Access Token 갱신",
            description = """
                    Refresh Token을 사용해 새로운 Access Token을 발급받습니다.
                    
                    **요구사항:**
                    - 쿠키에 refreshToken 필요
                    
                    **응답:**
                    - 새로운 Access Token (JSON + Cookie)
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "토큰 갱신 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TokenResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                      "message": "토큰이 갱신되었습니다."
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Refresh Token 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "accessToken": null,
                                      "message": "Refresh Token이 없습니다."
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않거나 만료된 Refresh Token",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "status": 401,
                                      "code": "AUTH002",
                                      "message": "유효하지 않은 Refresh Token입니다."
                                    }
                                    """)
                    )
            )
    })
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        // 1. 쿠키에서 Refresh Token 추출
        String refreshToken = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        if (refreshToken == null) {
            return ResponseEntity.badRequest().body(new TokenResponse(null, "Refresh Token이 없습니다."));
        }

        // 2. 새로운 Access Token과 Refresh Token 생성 (Rotation)
        String[] tokens = authService.refreshAccessToken(refreshToken);
        String newAccessToken = tokens[0];
        String newRefreshToken = tokens[1];

        // 3. 새로운 Access Token을 쿠키에 설정
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", newAccessToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(1800) // 30분
                .sameSite("Lax")
                .build();
        response.addHeader("Set-Cookie", accessCookie.toString());
        
        // 4. 새로운 Refresh Token을 쿠키에 설정 (Rotation)
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", newRefreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(604800) // 7일
                .sameSite("Lax")
                .build();
        response.addHeader("Set-Cookie", refreshCookie.toString());

        return ResponseEntity.ok(new TokenResponse(newAccessToken, "토큰이 갱신되었습니다."));
    }

    @Operation(
            summary = "로그아웃",
            description = """
                    로그아웃 후 쿠키와 Redis의 Refresh Token을 삭제합니다.
                    
                    **요구사항:**
                    - 로그인 상태 (Access Token 필요)
                    
                    **처리 내용:**
                    - Redis에서 Refresh Token 삭제
                    - 브라우저 쿠키 삭제 (accessToken, refreshToken)
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "로그아웃 성공",
                    content = @Content(
                            mediaType = "text/plain",
                            examples = @ExampleObject(value = "로그아웃되었습니다.")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "로그인 상태가 아님",
                    content = @Content(
                            mediaType = "text/plain",
                            examples = @ExampleObject(value = "로그인 상태가 아닙니다.")
                    )
            )
    })
    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @Parameter(hidden = true) Authentication authentication,
            HttpServletResponse response) {
        if (authentication == null) {
            return ResponseEntity.badRequest().body("로그인 상태가 아닙니다.");
        }

        String email = authentication.getName();

        // 1. Redis에서 Refresh Token 삭제
        authService.logout(email);

        // 2. Access Token 쿠키 삭제
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader("Set-Cookie", accessCookie.toString());

        // 3. Refresh Token 쿠키 삭제
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader("Set-Cookie", refreshCookie.toString());

        return ResponseEntity.ok("로그아웃되었습니다.");
    }
}
