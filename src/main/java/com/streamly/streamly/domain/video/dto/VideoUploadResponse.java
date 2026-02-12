package com.streamly.streamly.domain.video.dto;

import com.streamly.streamly.domain.video.entity.VideoStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoUploadResponse {

    private Long videoId;
    private String title;
    private VideoStatus status;
    private String message;
}
