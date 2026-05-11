package com.streamly.streamly.domain.advertiser.dto;

import com.streamly.streamly.domain.advertiser.entity.AdObjectCategory;
import com.streamly.streamly.domain.advertiser.entity.AdVideo;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class AdVideoDto {

    // 업로드 요청
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadRequest {
        private String title;
        private String description;
        private String objectCategory;
    }

    // 목록/상세 응답
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String title;
        private String description;
        private String originalFilename;
        private Long originalFileSize;
        private String status;
        private String failReason;
        private String nukiDirPath;
        private String objectCategory;
        private Long advertiserId;
        private String advertiserNickname;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(AdVideo adVideo) {
            return Response.builder()
                    .id(adVideo.getId())
                    .title(adVideo.getTitle())
                    .description(adVideo.getDescription())
                    .originalFilename(adVideo.getOriginalFilename())
                    .originalFileSize(adVideo.getOriginalFileSize())
                    .status(adVideo.getStatus().name())
                    .failReason(adVideo.getFailReason())
                    .nukiDirPath(adVideo.getNukiDirPath())
                    .objectCategory(adVideo.getObjectCategory() != null ? adVideo.getObjectCategory().name() : null)
                    .advertiserId(adVideo.getAdvertiser().getId())
                    .advertiserNickname(adVideo.getAdvertiser().getNickname())
                    .createdAt(adVideo.getCreatedAt())
                    .updatedAt(adVideo.getUpdatedAt())
                    .build();
        }
    }

    // 누끼 이미지 목록 응답
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NukiImagesResponse {
        private Long adVideoId;
        private String status;
        private List<String> imageUrls;
    }

    // AI 콜백 수신 (AI 서버 → BE)
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NukiCallbackRequest {
        private Long adVideoId;
        private boolean success;
        private String nukiDirPath; // 처리 성공 시 누끼 저장 경로
        private String failReason;  // 처리 실패 시 사유
    }
}
