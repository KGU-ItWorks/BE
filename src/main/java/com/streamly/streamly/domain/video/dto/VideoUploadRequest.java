package com.streamly.streamly.domain.video.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoUploadRequest {

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다.")
    private String title;

    @Size(max = 5000, message = "설명은 5000자를 초과할 수 없습니다.")
    private String description;

    @Size(max = 50, message = "카테고리는 50자를 초과할 수 없습니다.")
    private String category;

    @Size(max = 20, message = "연령 등급은 20자를 초과할 수 없습니다.")
    private String ageRating;
}
