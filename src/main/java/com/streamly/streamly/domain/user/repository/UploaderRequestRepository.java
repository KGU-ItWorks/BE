package com.streamly.streamly.domain.user.repository;

import com.streamly.streamly.domain.user.entity.UploaderRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UploaderRequestRepository extends JpaRepository<UploaderRequest, Long> {
    
    /**
     * 특정 사용자의 대기 중인 신청 조회
     */
    @Query("SELECT r FROM UploaderRequest r WHERE r.user.id = :userId AND r.status = 'PENDING'")
    Optional<UploaderRequest> findPendingRequestByUserId(@Param("userId") Long userId);
    
    /**
     * 특정 사용자의 모든 신청 내역 조회
     */
    @Query("SELECT r FROM UploaderRequest r WHERE r.user.id = :userId ORDER BY r.createdAt DESC")
    Page<UploaderRequest> findByUserId(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * 상태별 신청 조회 (관리자용)
     */
    @Query("SELECT r FROM UploaderRequest r WHERE r.status = :status ORDER BY r.createdAt DESC")
    Page<UploaderRequest> findByStatus(@Param("status") UploaderRequest.RequestStatus status, Pageable pageable);
    
    /**
     * 모든 신청 조회 (관리자용)
     */
    @Query("SELECT r FROM UploaderRequest r ORDER BY r.createdAt DESC")
    Page<UploaderRequest> findAllRequests(Pageable pageable);
    
    /**
     * 대기 중인 신청 개수
     */
    @Query("SELECT COUNT(r) FROM UploaderRequest r WHERE r.status = 'PENDING'")
    long countPendingRequests();
}
