package com.streamly.streamly.domain.video.repository;

import com.streamly.streamly.domain.video.entity.ApprovalStatus;
import com.streamly.streamly.domain.video.entity.Video;
import com.streamly.streamly.domain.video.entity.VideoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VideoRepository extends JpaRepository<Video, Long> {

    // 공개된 영상만 조회 (완료 + 승인)
    @Query("SELECT v FROM Video v WHERE v.status = 'COMPLETED' AND v.approvalStatus = 'APPROVED'")
    Page<Video> findPublishedVideos(Pageable pageable);

    // 카테고리별 공개 영상
    @Query("SELECT v FROM Video v WHERE v.status = 'COMPLETED' AND v.approvalStatus = 'APPROVED' AND v.category = :category")
    Page<Video> findPublishedVideosByCategory(@Param("category") String category, Pageable pageable);

    // 특정 사용자의 업로드 영상
    @Query("SELECT v FROM Video v WHERE v.uploader.id = :uploaderId")
    Page<Video> findByUploaderId(@Param("uploaderId") Long uploaderId, Pageable pageable);

    // 승인 대기 중인 영상 (관리자용)
    Page<Video> findByApprovalStatus(ApprovalStatus approvalStatus, Pageable pageable);

    // 검색 (제목 + 설명)
    @Query("SELECT v FROM Video v WHERE v.status = 'COMPLETED' AND v.approvalStatus = 'APPROVED' " +
           "AND (LOWER(v.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(v.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Video> searchPublishedVideos(@Param("keyword") String keyword, Pageable pageable);

    // 조회수 상위 영상
    @Query("SELECT v FROM Video v WHERE v.status = 'COMPLETED' AND v.approvalStatus = 'APPROVED' " +
           "ORDER BY v.viewCount DESC")
    Page<Video> findTopViewedVideos(Pageable pageable);

    // 최근 업로드 영상
    @Query("SELECT v FROM Video v WHERE v.status = 'COMPLETED' AND v.approvalStatus = 'APPROVED' " +
           "ORDER BY v.publishedAt DESC")
    Page<Video> findRecentVideos(Pageable pageable);

    // 특정 상태의 영상 개수
    long countByStatus(VideoStatus status);

    // 승인 상태별 영상 개수
    long countByApprovalStatus(ApprovalStatus approvalStatus);

    // 업로더별 영상 개수
    long countByUploaderId(Long uploaderId);

    // 상태별 영상 조회 (관리자용)
    Page<Video> findByStatus(VideoStatus status, Pageable pageable);

    // 상태와 승인 상태 모두로 필터링 (관리자용)
    Page<Video> findByStatusAndApprovalStatus(VideoStatus status, ApprovalStatus approvalStatus, Pageable pageable);

    // 전체 조회수 합계
    @Query("SELECT SUM(v.viewCount) FROM Video v")
    Long sumViewCount();

    // 전체 저장 용량 합계
    @Query("SELECT SUM(v.originalFileSize) FROM Video v")
    Long sumOriginalFileSize();
}
