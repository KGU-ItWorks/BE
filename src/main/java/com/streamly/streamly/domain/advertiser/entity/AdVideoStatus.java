package com.streamly.streamly.domain.advertiser.entity;

public enum AdVideoStatus {
    PENDING,     // 업로드 완료, AI 처리 대기
    PROCESSING,  // AI 누끼 처리 중
    DONE,        // 처리 완료
    FAILED       // 처리 실패
}
