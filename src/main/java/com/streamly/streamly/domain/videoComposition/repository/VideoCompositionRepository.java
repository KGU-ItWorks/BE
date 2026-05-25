package com.streamly.streamly.domain.videoComposition.repository;

import com.streamly.streamly.domain.videoComposition.entity.AdvertiserApprovalStatus;
import com.streamly.streamly.domain.videoComposition.entity.CompositionStatus;
import com.streamly.streamly.domain.videoComposition.entity.VideoComposition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VideoCompositionRepository extends JpaRepository<VideoComposition, Long> {

    Optional<VideoComposition> findFirstByVideoIdAndStatusOrderByRequestedAtDesc(
            Long videoId, CompositionStatus status);

    /** Used by PlaylistResolverService — only serve compositions the advertiser has approved. */
    Optional<VideoComposition> findFirstByVideoIdAndStatusAndApprovalStatusOrderByPublishedAtDesc(
            Long videoId, CompositionStatus status, AdvertiserApprovalStatus approvalStatus);

    List<VideoComposition> findByStatusAndRequestedAtBefore(
            CompositionStatus status, LocalDateTime cutoff);

    List<VideoComposition> findByStatusAndLastHeartbeatAtBefore(
            CompositionStatus status, LocalDateTime cutoff);

    Page<VideoComposition> findByApprovalStatusAndAdvertiserId(
            AdvertiserApprovalStatus status, Long advertiserId, Pageable pageable);

    List<VideoComposition> findByStatus(CompositionStatus status);
}
