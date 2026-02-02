package com.streamly.streamly.domain.video.entity;

public enum VideoStatus {
    UPLOADING,      // 업로드 중
    UPLOADED,       // 업로드 완료, 인코딩 대기
    ENCODING,       // 인코딩 중
    COMPLETED,      // 인코딩 완료, 스트리밍 가능
    FAILED,         // 처리 실패
    DELETED         // 삭제됨
}
