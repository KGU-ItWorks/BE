package com.streamly.streamly.global.exception.video;

import com.streamly.streamly.global.exception.BusinessException;
import com.streamly.streamly.global.exception.ErrorCode;

/**
 * 비디오 업로드에 실패했을 때 발생하는 예외
 */
public class VideoUploadException extends BusinessException {
    public VideoUploadException() {
        super(ErrorCode.VIDEO_UPLOAD_FAILED);
    }

    public VideoUploadException(String message) {
        super(ErrorCode.VIDEO_UPLOAD_FAILED, message);
    }
}
