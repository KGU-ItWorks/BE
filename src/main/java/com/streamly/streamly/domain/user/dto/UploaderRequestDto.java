package com.streamly.streamly.domain.user.dto;

import com.streamly.streamly.domain.user.entity.UploaderRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 업로더 승급 신청 DTO
 */
public class UploaderRequestDto {
    
    // Request
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Create {
        private String reason; // 신청 이유
    }
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Reject {
        private String comment; // 거부 사유
    }
    
    // Response
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
        
        public static Response from(UploaderRequest request) {
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
