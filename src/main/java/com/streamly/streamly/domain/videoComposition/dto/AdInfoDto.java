package com.streamly.streamly.domain.videoComposition.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdInfoDto {
    private boolean hasAd;
    private Long adVideoId;
    private String advertiserName;
    private String description;
    private String nukiImageUrl;

    public static AdInfoDto from(boolean hasAd, Long adVideoId, String advertiserName, String description, String nukiImageUrl) {
        return AdInfoDto.builder()
                .hasAd(hasAd)
                .adVideoId(adVideoId)
                .advertiserName(advertiserName)
                .description(description)
                .nukiImageUrl(nukiImageUrl)
                .build();
    }

    public static AdInfoDto noAd() {
        return AdInfoDto.builder()
                .hasAd(false)
                .build();
    }
}
