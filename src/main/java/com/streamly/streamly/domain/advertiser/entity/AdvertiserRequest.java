package com.streamly.streamly.domain.advertiser.entity;

import com.streamly.streamly.domain.user.entity.User;
import com.streamly.streamly.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 광고주 권한 신청 엔티티 - UploaderRequest와 동일한 구조
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "advertiser_requests")
public class AdvertiserRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    @Column(length = 1000)
    private String adminComment;

    @Column
    private Long processedByAdminId;

    @Builder
    public AdvertiserRequest(User user, String reason) {
        this.user = user;
        this.reason = reason;
        this.status = RequestStatus.PENDING;
    }

    public void approve(Long adminId) {
        this.status = RequestStatus.APPROVED;
        this.processedByAdminId = adminId;
    }

    public void reject(Long adminId, String comment) {
        this.status = RequestStatus.REJECTED;
        this.adminComment = comment;
        this.processedByAdminId = adminId;
    }

    public enum RequestStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
}
