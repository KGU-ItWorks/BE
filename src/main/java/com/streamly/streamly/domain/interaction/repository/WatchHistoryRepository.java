package com.streamly.streamly.domain.interaction.repository;

import com.streamly.streamly.domain.interaction.entity.WatchHistory;
import com.streamly.streamly.domain.user.entity.User;
import com.streamly.streamly.domain.video.entity.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface WatchHistoryRepository extends JpaRepository<WatchHistory, Long> {

    // 시청 기록 목록 (페이지네이션, 최근 시청 순 정렬용)
    Page<WatchHistory> findByUser(User user, Pageable pageable);

    // 특정 영상의 시청 기록 조회 (upsert 용)
    Optional<WatchHistory> findByUserAndVideo(User user, Video video);

    // 시청 기록 삭제
    @Modifying
    int deleteByUserIdAndVideoId(Long userId, Long videoId);

    // 시청 기록 전체 삭제
    @Modifying
    void deleteByUser(User user);

    @Query(value = "SELECT wh FROM WatchHistory wh JOIN FETCH wh.video v JOIN FETCH v.uploader WHERE wh.user = :user",
           countQuery = "SELECT COUNT(wh) FROM WatchHistory wh WHERE wh.user = :user")
    Page<WatchHistory> findByUserWithVideoAndUploader(User user, Pageable pageable);
}
