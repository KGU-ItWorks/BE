package com.streamly.streamly.domain.interaction.dto;

import com.streamly.streamly.domain.interaction.entity.Favorites;
import com.streamly.streamly.domain.video.dto.VideoResponse;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FavoritesResponse {
    private Long favoriteId;
    private LocalDateTime createdAt;
    private VideoResponse video;

    public static FavoritesResponse from(Favorites favorites) {
        return FavoritesResponse.builder()
                .favoriteId(favorites.getId())
                .createdAt(favorites.getCreatedAt())
                .video(VideoResponse.fromSimple(favorites.getVideo()))
                .build();
    }
}
