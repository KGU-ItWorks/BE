package com.streamly.streamly.domain.video.service;

import com.streamly.streamly.domain.video.dto.AiFetchRequest;
import com.streamly.streamly.domain.video.dto.AiFetchResponse;
import com.streamly.streamly.domain.video.entity.Video;
import com.streamly.streamly.domain.video.repository.VideoRepository;
import com.streamly.streamly.global.exception.video.VideoNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiVideoService {
    private final VideoRepository videoRepository;
    private final WebClient webClient;

    @Value("${ai.server.url}")
    private String aiServerUrl;

    @Value("${ai.server.api-prefix:/api/v1}")
    private String aiApiPrefix;

    @Value("${server.base-url:http://localhost:8080}")
    private String beServerUrl;

    public AiFetchResponse requestAiFetch(Long videoId,String startTime,Integer duration){
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new VideoNotFoundException("영상을 찾을 수 없습니다. id=" + videoId));

        String rawUrl = video.getCloudfrontUrl() != null
                ? video.getCloudfrontUrl()
                : video.getS3Url();

        String videoUrl = rawUrl.startsWith("/")
                ? beServerUrl + rawUrl
                : rawUrl;

        AiFetchRequest request = AiFetchRequest.from(videoId, videoUrl, startTime != null ? startTime : "00:00:00", duration);

        log.info("AI 서버 fetch 요청 - videoId: {}, url: {}", videoId, videoUrl);

        AiFetchResponse response = webClient.post()
                .uri(aiServerUrl + aiApiPrefix + "/video/fetch")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AiFetchResponse.class)
                .block();
        log.info("AI 서버 fetch 응답 - taskId: {}, ai server message: {}"
                ,response != null ? response.getTaskId() : "응답 실패",response != null ? response.getMessage() : "응답 실패");

        return response;
    }
}
