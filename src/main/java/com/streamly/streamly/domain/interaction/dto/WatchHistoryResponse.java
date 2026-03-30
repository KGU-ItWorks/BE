package com.streamly.streamly.domain.interaction.dto;

import com.streamly.streamly.domain.interaction.entity.WatchHistory;
import com.streamly.streamly.domain.video.dto.VideoResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WatchHistoryResponse {
    private Long historyId;
    private Integer lastPositionSeconds; // 이어보기용
    private LocalDateTime watchedAt;     // 최근 시청 시각
    private VideoResponse video;

    public static WatchHistoryResponse from(WatchHistory watchHistory) {
        return WatchHistoryResponse.builder()
                .historyId(watchHistory.getId())
                .lastPositionSeconds(watchHistory.getLastPositionSeconds())
                .watchedAt(watchHistory.getWatchedAt())
                .video(VideoResponse.fromSimple(watchHistory.getVideo()))
                .build();
    }
}
