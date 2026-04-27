package com.streamly.streamly.domain.advertiser.repository;

import com.streamly.streamly.domain.advertiser.entity.AdvertiserRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AdvertiserRequestRepository extends JpaRepository<AdvertiserRequest, Long> {

    @Query("SELECT r FROM AdvertiserRequest r WHERE r.user.id = :userId AND r.status = 'PENDING'")
    Optional<AdvertiserRequest> findPendingRequestByUserId(@Param("userId") Long userId);

    @Query("SELECT r FROM AdvertiserRequest r JOIN FETCH r.user WHERE r.user.id = :userId ORDER BY r.createdAt DESC")
    Page<AdvertiserRequest> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT r FROM AdvertiserRequest r JOIN FETCH r.user ORDER BY r.createdAt DESC")
    Page<AdvertiserRequest> findAllRequests(Pageable pageable);

    @Query("SELECT r FROM AdvertiserRequest r JOIN FETCH r.user WHERE r.status = :status ORDER BY r.createdAt DESC")
    Page<AdvertiserRequest> findByStatus(@Param("status") AdvertiserRequest.RequestStatus status, Pageable pageable);

    @Query("SELECT COUNT(r) FROM AdvertiserRequest r WHERE r.status = 'PENDING'")
    long countPendingRequests();
}
