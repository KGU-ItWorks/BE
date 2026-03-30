package com.streamly.streamly.domain.interaction.entity;

import com.streamly.streamly.domain.user.entity.User;
import com.streamly.streamly.domain.video.entity.Video;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "watch_history",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "video_id"})
        }
)
public class WatchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(name = "last_position_seconds")
    @Builder.Default
    private Integer lastPositionSeconds = 0;

    @Column(name = "watched_at", nullable = false)
    private LocalDateTime watchedAt;

    // 시청 기록 업데이트 (재시청 시 호출)
    public void update(Integer lastPositionSeconds) {
        this.lastPositionSeconds = lastPositionSeconds;
        this.watchedAt = LocalDateTime.now();
    }
}
