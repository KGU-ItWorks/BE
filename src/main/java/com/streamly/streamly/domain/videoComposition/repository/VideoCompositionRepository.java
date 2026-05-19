package com.streamly.streamly.domain.videoComposition.repository;

import com.streamly.streamly.domain.videoComposition.entity.CompositionStatus;
import com.streamly.streamly.domain.videoComposition.entity.VideoComposition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VideoCompositionRepository extends JpaRepository<VideoComposition, Long> {

    Optional<VideoComposition> findFirstByVideoIdAndStatusOrderByRequestedAtDesc(
            Long videoId, CompositionStatus status);

    List<VideoComposition> findByStatusAndRequestedAtBefore(
            CompositionStatus status, LocalDateTime cutoff);

    List<VideoComposition> findByStatusAndLastHeartbeatAtBefore(
            CompositionStatus status, LocalDateTime cutoff);

    List<VideoComposition> findByStatus(CompositionStatus status);
}
