package com.streamly.streamly.global.exception.video;

import com.streamly.streamly.global.exception.BusinessException;
import com.streamly.streamly.global.exception.ErrorCode;

/**
 * 비디오를 찾을 수 없을 때 발생하는 예외
 */
public class VideoNotFoundException extends BusinessException {
    public VideoNotFoundException() {
        super(ErrorCode.VIDEO_NOT_FOUND);
    }

    public VideoNotFoundException(String message) {
        super(ErrorCode.VIDEO_NOT_FOUND, message);
    }
}
