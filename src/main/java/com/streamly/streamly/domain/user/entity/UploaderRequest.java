package com.streamly.streamly.domain.user.entity;

import com.streamly.streamly.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 업로더 승급 신청 엔티티
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "uploader_requests")
public class UploaderRequest extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false, length = 1000)
    private String reason; // 승급 신청 이유
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status; // 신청 상태
    
    @Column(length = 1000)
    private String adminComment; // 관리자 의견
    
    @Column
    private Long processedByAdminId; // 처리한 관리자 ID
    
    @Builder
    public UploaderRequest(User user, String reason) {
        this.user = user;
        this.reason = reason;
        this.status = RequestStatus.PENDING;
    }
    
    /**
     * 승급 승인
     */
    public void approve(Long adminId) {
        this.status = RequestStatus.APPROVED;
        this.processedByAdminId = adminId;
    }
    
    /**
     * 승급 거부
     */
    public void reject(Long adminId, String comment) {
        this.status = RequestStatus.REJECTED;
        this.adminComment = comment;
        this.processedByAdminId = adminId;
    }
    
    public enum RequestStatus {
        PENDING,   // 대기 중
        APPROVED,  // 승인됨
        REJECTED   // 거부됨
    }
}
