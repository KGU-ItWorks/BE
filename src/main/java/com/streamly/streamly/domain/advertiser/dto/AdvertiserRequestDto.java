package com.streamly.streamly.domain.advertiser.dto;

import com.streamly.streamly.domain.advertiser.entity.AdvertiserRequest;
import lombok.*;

import java.time.LocalDateTime;

public class AdvertiserRequestDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Create {
        private String reason;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Reject {
        private String comment;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long userId;
        private String userEmail;
        private String userNickname;
        private String reason;
        private String status;
        private String adminComment;
        private Long processedByAdminId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(AdvertiserRequest request) {
            return Response.builder()
                    .id(request.getId())
                    .userId(request.getUser().getId())
                    .userEmail(request.getUser().getEmail())
                    .userNickname(request.getUser().getNickname())
                    .reason(request.getReason())
                    .status(request.getStatus().name())
                    .adminComment(request.getAdminComment())
                    .processedByAdminId(request.getProcessedByAdminId())
                    .createdAt(request.getCreatedAt())
                    .updatedAt(request.getUpdatedAt())
                    .build();
        }
    }
}
