package com.streamly.streamly.global.exception.auth;

import com.streamly.streamly.global.exception.BusinessException;
import com.streamly.streamly.global.exception.ErrorCode;

/**
 * 이메일 또는 비밀번호가 올바르지 않을 때 발생하는 예외
 */
public class InvalidCredentialsException extends BusinessException {
    public InvalidCredentialsException() {
        super(ErrorCode.INVALID_CREDENTIALS);
    }

    public InvalidCredentialsException(String message) {
        super(ErrorCode.INVALID_CREDENTIALS, message);
    }
}
