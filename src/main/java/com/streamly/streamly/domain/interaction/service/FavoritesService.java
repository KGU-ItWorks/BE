package com.streamly.streamly.domain.interaction.service;

import com.streamly.streamly.domain.interaction.dto.FavoritesResponse;
import com.streamly.streamly.domain.interaction.entity.Favorites;
import com.streamly.streamly.domain.interaction.repository.FavoritesRepository;
import com.streamly.streamly.domain.user.entity.User;
import com.streamly.streamly.domain.user.repository.UserRepository;
import com.streamly.streamly.domain.video.entity.Video;
import com.streamly.streamly.domain.video.repository.VideoRepository;
import com.streamly.streamly.global.exception.user.UserNotFoundException;
import com.streamly.streamly.global.exception.video.VideoNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoritesService {
    private final FavoritesRepository favoritesRepository;
    private final UserRepository userRepository;
    private final VideoRepository videoRepository;

    public Page<FavoritesResponse> getFavoriteVideos(String email, Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        return favoritesRepository.findByUserWithVideoAndUploader(user, pageable)
                .map(FavoritesResponse::from);
    }

    @Transactional
    public boolean toggleFavorite(String email, Long videoId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new VideoNotFoundException("영상을 찾을 수 없습니다."));

        int deleted = favoritesRepository.deleteByUserIdAndVideoId(user.getId(), videoId);
        if (deleted == 0) {
            // 찜 목록에 없었음 → 추가
            favoritesRepository.save(Favorites.builder()
                    .user(user)
                    .video(video)
                    .build());
            return true;  // true = 찜 추가됨
        }
        return false;     // false = 찜 취소됨
    }
}
