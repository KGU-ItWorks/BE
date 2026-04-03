package com.streamly.streamly.domain.interaction.service;

import com.streamly.streamly.domain.interaction.dto.WatchHistoryResponse;
import com.streamly.streamly.domain.interaction.entity.WatchHistory;
import com.streamly.streamly.domain.interaction.repository.WatchHistoryRepository;
import com.streamly.streamly.domain.user.entity.User;
import com.streamly.streamly.domain.user.repository.UserRepository;
import com.streamly.streamly.domain.video.entity.Video;
import com.streamly.streamly.domain.video.repository.VideoRepository;
import com.streamly.streamly.global.exception.user.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WatchHistoryService {

    private final WatchHistoryRepository watchHistoryRepository;
    private final UserRepository userRepository;
    private final VideoRepository videoRepository;

    // 시청 기록 목록 조회
    public Page<WatchHistoryResponse> getWatchHistory(String email, Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        return watchHistoryRepository.findByUserWithVideoAndUploader(user, pageable)
                .map(WatchHistoryResponse::from);
    }

    // 시청 기록 저장 (upsert — 이미 있으면 업데이트, 없으면 생성)
    @Transactional
    public WatchHistoryResponse recordWatch(String email, Long videoId, Integer lastPositionSeconds) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("영상을 찾을 수 없습니다."));

        WatchHistory watchHistory = watchHistoryRepository.findByUserAndVideo(user, video)
                .map(existing -> {
                    existing.update(lastPositionSeconds);
                    return existing;
                })
                .orElseGet(() -> watchHistoryRepository.save(
                        WatchHistory.builder()
                                .user(user)
                                .video(video)
                                .lastPositionSeconds(lastPositionSeconds)
                                .watchedAt(LocalDateTime.now())
                                .build()
                ));

        return WatchHistoryResponse.from(watchHistory);
    }

    // 특정 시청 기록 삭제
    @Transactional
    public void deleteWatchHistory(String email, Long videoId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        int deleted = watchHistoryRepository.deleteByUserIdAndVideoId(user.getId(), videoId);
        if (deleted == 0) {
            throw new IllegalArgumentException("시청 기록을 찾을 수 없습니다.");
        }
    }

    // 시청 기록 전체 삭제
    @Transactional
    public void clearAllWatchHistory(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        watchHistoryRepository.deleteByUser(user);
    }
}
