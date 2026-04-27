package com.streamly.streamly.domain.advertiser.entity;

import com.streamly.streamly.domain.user.entity.User;
import com.streamly.streamly.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 광고주 전용 영상 엔티티 - 일반 Video와 완전히 분리된 임시 처리용 테이블
 * AI 누끼 처리 완료 후 삭제되며, 관리자 영상 관리 화면에 노출되지 않음
 */
@Entity
@Table(name = "ad_videos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AdVideo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advertiser_id", nullable = false)
    private User advertiser;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "original_file_size")
    private Long originalFileSize;

    // 로컬 저장 경로 (S3 대체)
    @Column(name = "file_path", nullable = false)
    private String filePath;

    // AI 처리 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AdVideoStatus status = AdVideoStatus.PENDING;

    // AI 처리 실패 사유
    @Column(name = "fail_reason")
    private String failReason;

    // 누끼 결과 로컬 저장 디렉토리 경로 (처리 완료 후 설정)
    @Column(name = "nuki_dir_path")
    private String nukiDirPath;

    // 비즈니스 메서드
    public void markProcessing() {
        this.status = AdVideoStatus.PROCESSING;
    }

    public void markDone(String nukiDirPath) {
        this.status = AdVideoStatus.DONE;
        this.nukiDirPath = nukiDirPath;
    }

    public void markFailed(String reason) {
        this.status = AdVideoStatus.FAILED;
        this.failReason = reason;
    }
}
