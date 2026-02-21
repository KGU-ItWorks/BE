package com.streamly.streamly.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 에러 코드 정의
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // 인증 관련 (401)
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH001", "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH002", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH003", "만료된 토큰입니다."),
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH004", "토큰을 찾을 수 없습니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH005", "Refresh Token을 찾을 수 없습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH006", "인증이 필요합니다."),

    // 권한 관련 (403)
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH007", "접근 권한이 없습니다."),

    // 사용자 관련 (404, 409)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER001", "사용자를 찾을 수 없습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER002", "이미 사용 중인 이메일입니다."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER003", "이미 사용 중인 닉네임입니다."),

    // 비디오 관련 (404, 400)
    VIDEO_NOT_FOUND(HttpStatus.NOT_FOUND, "VIDEO001", "비디오를 찾을 수 없습니다."),
    INVALID_VIDEO_FORMAT(HttpStatus.BAD_REQUEST, "VIDEO002", "지원하지 않는 비디오 형식입니다."),
    VIDEO_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "VIDEO003", "비디오 업로드에 실패했습니다."),
    VIDEO_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "VIDEO004", "비디오 크기가 제한을 초과했습니다."),

    // 요청 검증 (400)
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON001", "잘못된 입력 값입니다."),
    MISSING_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, "COMMON002", "필수 요청 파라미터가 누락되었습니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "COMMON003", "잘못된 타입의 값입니다."),

    // 서버 오류 (500)
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON004", "서버 내부 오류가 발생했습니다."),
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON005", "데이터베이스 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
