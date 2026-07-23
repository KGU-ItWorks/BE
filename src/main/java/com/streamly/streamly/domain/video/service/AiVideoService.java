package com.streamly.streamly.domain.video.service;

import com.streamly.streamly.domain.user.entity.Role;
import com.streamly.streamly.domain.user.entity.User;
import com.streamly.streamly.domain.user.repository.UserRepository;
import com.streamly.streamly.domain.video.dto.VideoComposeMessage;
import com.streamly.streamly.domain.video.entity.Video;
import com.streamly.streamly.domain.video.repository.VideoRepository;
import com.streamly.streamly.domain.videoComposition.entity.BoundingBox;
import com.streamly.streamly.domain.videoComposition.service.VideoCompositionService;
import com.streamly.streamly.global.config.RabbitMQConfig;
import com.streamly.streamly.global.exception.BusinessException;
import com.streamly.streamly.global.exception.ErrorCode;
import com.streamly.streamly.global.exception.user.UserNotFoundException;
import com.streamly.streamly.global.exception.video.VideoNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiVideoService {

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final RabbitTemplate rabbitTemplate;
    private final VideoCompositionService videoCompositionService;

    @Value("${server.base-url:http://localhost:8080}")
    private String beServerUrl;

    public Long requestAiComposition(String requesterEmail, Long videoId, String startTime, Integer duration, BoundingBox boundingBox) {
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new VideoNotFoundException("영상을 찾을 수 없습니다. id=" + videoId));

        // IDOR 방어: ADMIN이 아닌 경우 본인 소유 영상만 요청 가능
        boolean isAdmin = requester.getRole() == Role.ROLE_ADMIN;
        boolean isOwner = video.getUploader().getId().equals(requester.getId());
        if (!isAdmin && !isOwner) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "본인이 업로드한 영상에 대해서만 AI 합성을 요청할 수 있습니다.");
        }

        // NPE 방어: 스트리밍 URL이 없으면 처리 불가
        String rawUrl = video.getCloudfrontUrl() != null ? video.getCloudfrontUrl() : video.getS3Url();
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "영상 URL이 존재하지 않습니다. 인코딩이 완료된 영상인지 확인하세요.");
        }
        String videoUrl = rawUrl.startsWith("/") ? beServerUrl + rawUrl : rawUrl;

        VideoComposeMessage draft = VideoComposeMessage.draft(videoId, videoUrl, startTime, duration, boundingBox);
        Long compositionId = videoCompositionService.initialSave(draft);

        String callbackUrl = beServerUrl + "/api/v1/video-compositions/" + compositionId + "/callback";
        VideoComposeMessage message = VideoComposeMessage.withCallback(draft, callbackUrl);

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.VIDEO_FETCH_EXCHANGE,
                    RabbitMQConfig.VIDEO_FETCH_ROUTING_KEY,
                    message
            );
            log.info("AI 합성 요청 - videoId: {}, compositionId: {}, callbackUrl: {}",
                    videoId, compositionId, callbackUrl);
            return compositionId;
        } catch (Exception e) {
            log.error("RabbitMQ 메시지 발행 실패 - compositionId: {}, error: {}", compositionId, e.getMessage());
            videoCompositionService.markFailed(compositionId, "MQ 발행 실패: " + e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "AI 합성 요청 중 오류가 발생했습니다.");
        }
    }
}
