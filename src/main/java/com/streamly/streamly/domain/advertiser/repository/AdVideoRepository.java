package com.streamly.streamly.domain.advertiser.repository;

import com.streamly.streamly.domain.advertiser.entity.AdVideo;
import com.streamly.streamly.domain.advertiser.entity.AdVideoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdVideoRepository extends JpaRepository<AdVideo, Long> {

    // 광고주 본인 영상 목록
    @Query("SELECT a FROM AdVideo a JOIN FETCH a.advertiser WHERE a.advertiser.id = :advertiserId")
    Page<AdVideo> findByAdvertiserId(@Param("advertiserId") Long advertiserId, Pageable pageable);

    // 관리자용 전체 조회 (광고주 정보 포함)
    @Query("SELECT a FROM AdVideo a JOIN FETCH a.advertiser")
    Page<AdVideo> findAllWithAdvertiser(Pageable pageable);

    // 관리자용 상태 필터 조회
    @Query("SELECT a FROM AdVideo a JOIN FETCH a.advertiser WHERE a.status = :status")
    Page<AdVideo> findByStatus(@Param("status") AdVideoStatus status, Pageable pageable);

    long countByStatus(AdVideoStatus status);
}
