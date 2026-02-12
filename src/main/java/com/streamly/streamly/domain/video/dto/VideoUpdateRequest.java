package com.streamly.streamly.domain.video.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "영상 정보 수정 요청 DTO")
public class VideoUpdateRequest {
    
    @Schema(description = "영상 제목", example = "새로운 영상 제목", required = true)
    @NotBlank(message = "제목은 필수입니다")
    @Size(min = 1, max = 200, message = "제목은 1자 이상 200자 이하여야 합니다")
    private String title;
    
    @Schema(description = "영상 설명", example = "영상에 대한 상세 설명")
    @Size(max = 5000, message = "설명은 5000자 이하여야 합니다")
    private String description;
    
    @Schema(description = "카테고리", example = "ACTION")
    @Size(max = 50, message = "카테고리는 50자 이하여야 합니다")
    private String category;
    
    @Schema(description = "연령 등급", example = "15+")
    @Size(max = 20, message = "연령 등급은 20자 이하여야 합니다")
    private String ageRating;
}
