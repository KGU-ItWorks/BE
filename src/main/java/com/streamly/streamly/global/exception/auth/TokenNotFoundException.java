package com.streamly.streamly.global.exception.auth;

import com.streamly.streamly.global.exception.BusinessException;
import com.streamly.streamly.global.exception.ErrorCode;

/**
 * Refresh Token을 찾을 수 없을 때 발생하는 예외
 */
public class TokenNotFoundException extends BusinessException {
    public TokenNotFoundException() {
        super(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
    }

    public TokenNotFoundException(String message) {
        super(ErrorCode.REFRESH_TOKEN_NOT_FOUND, message);
    }
}
