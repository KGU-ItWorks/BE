package com.streamly.streamly.domain.interaction.repository;

import com.streamly.streamly.domain.interaction.entity.Favorites;
import com.streamly.streamly.domain.user.entity.User;
import com.streamly.streamly.domain.video.entity.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface FavoritesRepository extends JpaRepository<Favorites,Long> {
    Page<Favorites> findByUser(User user, Pageable pageable);

    @Query("SELECT f FROM Favorites f JOIN FETCH f.video v JOIN FETCH v.uploader WHERE f.user = :user")
    Page<Favorites> findByUserWithVideoAndUploader(User user, Pageable pageable);

    @Modifying
    int deleteByUserIdAndVideoId(Long userId, Long videoId);


    //몇명이 영상을 찜했는지 카운팅
    long countByVideo(Video video);
}
