package com.streamly.streamly.domain.videoComposition.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamly.streamly.domain.advertiser.repository.AdVideoRepository;
import com.streamly.streamly.domain.user.entity.User;
import com.streamly.streamly.domain.user.repository.UserRepository;
import com.streamly.streamly.domain.video.dto.AiFetchResponse;
import com.streamly.streamly.domain.video.dto.VideoComposeMessage;
import com.streamly.streamly.domain.videoComposition.dto.VideoCompositionDto;
import com.streamly.streamly.domain.videoComposition.dto.VideoCompositionStatusResponse;
import com.streamly.streamly.domain.videoComposition.entity.AdvertiserApprovalStatus;
import com.streamly.streamly.domain.videoComposition.entity.CompositionStatus;
import com.streamly.streamly.domain.videoComposition.entity.VideoComposition;
import com.streamly.streamly.domain.videoComposition.repository.VideoCompositionRepository;
import com.streamly.streamly.global.exception.user.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoCompositionService {

    private final VideoCompositionRepository videoCompositionRepository;
    private final AdVideoRepository adVideoRepository;
    private final UserRepository userRepository;
    private final HeartbeatStore heartbeatStore;
    private final CompositionStatusPromoter statusPromoter;
    private final ObjectMapper objectMapper;

    public Optional<VideoComposition> getCurrentVideoComposition(Long videoId) {
        return videoCompositionRepository
                .findFirstByVideoIdAndStatusAndApprovalStatusOrderByPublishedAtDesc(videoId, CompositionStatus.COMPLETED, AdvertiserApprovalStatus.APPROVED);
    }

    // -------------------------------------------------------------------------
    // Advertiser — list pending compositions
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<VideoCompositionDto> getPendingComposedVideosByAdvertiser(String email, Pageable pageable) {
        User advertiser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
        return videoCompositionRepository
                .findByApprovalStatusAndAdvertiserId(AdvertiserApprovalStatus.PENDING, advertiser.getId(), pageable)
                .map(VideoCompositionDto::from);
    }

    // -------------------------------------------------------------------------
    // Advertiser — approve / reject
    // -------------------------------------------------------------------------

    @Transactional
    public void approve(Long compositionId, String advertiserEmail) {
        VideoComposition composition = findAndVerifyOwnership(compositionId, advertiserEmail);
        composition.approve();
        log.info("합성 영상 승인 - compositionId: {}, advertiser: {}", compositionId, advertiserEmail);
    }

    @Transactional
    public void reject(Long compositionId, String reason, String advertiserEmail) {
        VideoComposition composition = findAndVerifyOwnership(compositionId, advertiserEmail);
        composition.reject(reason);
        log.info("합성 영상 거절 - compositionId: {}, advertiser: {}", compositionId, advertiserEmail);
    }

    private VideoComposition findAndVerifyOwnership(Long compositionId, String advertiserEmail) {
        User advertiser = userRepository.findByEmail(advertiserEmail)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
        VideoComposition composition = videoCompositionRepository.findById(compositionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "VideoComposition을 찾을 수 없습니다. id=" + compositionId));
        if (!advertiser.getId().equals(composition.getAdvertiserId())) {
            throw new IllegalArgumentException("해당 합성 영상에 대한 권한이 없습니다.");
        }
        return composition;
    }

    // -------------------------------------------------------------------------
    // AI pipeline — save / callback / heartbeat / status
    // -------------------------------------------------------------------------

    @Transactional
    public Long initialSave(VideoComposeMessage message) {
        if (message.getVideoId() == null || message.getObjectPrompt() == null
                || message.getStartTime() == null || message.getDuration() == null) {
            throw new IllegalArgumentException("합성 요청 필수값이 누락되었습니다.");
        }
        VideoComposition composition = VideoComposition.builder()
                .videoId(message.getVideoId())
                .objectPrompt(message.getObjectPrompt())
                .startTime(message.getStartTime())
                .duration(message.getDuration())
                .build();
        Long id = videoCompositionRepository.save(composition).getId();
        heartbeatStore.trackQueued(id);
        return id;
    }

    @Transactional
    public void handleCallback(Long compositionId, AiFetchResponse response) {
        VideoComposition composition = videoCompositionRepository.findById(compositionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "VideoComposition을 찾을 수 없습니다. id=" + compositionId));

        if (composition.getStatus().isTerminal()) {
            log.info("중복 콜백 무시 - compositionId: {}, 현재 상태: {}", compositionId, composition.getStatus());
            return;
        }

        heartbeatStore.remove(compositionId);

        if (response.isSuccess()) {
            try {
                Long advertiserId = resolveAdvertiserId(response.getAdVideoId());
                String replacedSegmentsJson = toJson(response.getReplacedSegIndices());
                composition.markCompleted(
                        response.getTaskId(),
                        response.getAdVideoId(),
                        advertiserId,
                        response.getComposedPath(),
                        replacedSegmentsJson
                );
                log.info("합성 완료 - compositionId: {}, taskId: {}, adVideoId: {}, advertiserId: {}",
                        compositionId, response.getTaskId(), response.getAdVideoId(), advertiserId);
            } catch (IllegalArgumentException e) {
                composition.markFailed("광고 영상 정보를 찾을 수 없습니다: " + e.getMessage());
                log.warn("합성 콜백 처리 실패 - compositionId: {}, reason: {}", compositionId, e.getMessage());
            }
        } else {
            composition.markFailed(response.getFailReason());
            log.warn("합성 실패 - compositionId: {}, reason: {}", compositionId, response.getFailReason());
        }
    }

    @Transactional
    public void markProcessing(Long compositionId) {
        VideoComposition composition = videoCompositionRepository.findById(compositionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "VideoComposition을 찾을 수 없습니다. id=" + compositionId));
        if (!composition.getStatus().isTerminal()) {
            composition.markProcessing();
            log.info("합성 처리 시작 - compositionId: {}", compositionId);
        }
    }

    /**
     * Records the heartbeat in memory — no DB connection acquired on normal ticks.
     * On the first heartbeat, delegates to CompositionStatusPromoter (a separate
     * Spring bean) so that @Transactional is honoured via AOP proxy.
     */
    public void updateHeartbeat(Long compositionId) {
        boolean isFirst = heartbeatStore.recordAndIsFirst(compositionId);
        if (isFirst) {
            statusPromoter.promoteQueuedToProcessing(compositionId);
        }
    }

    @Transactional(readOnly = true)
    public VideoCompositionStatusResponse getStatus(Long compositionId) {
        VideoComposition composition = videoCompositionRepository.findById(compositionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "VideoComposition을 찾을 수 없습니다. id=" + compositionId));
        return VideoCompositionStatusResponse.from(composition);
    }

    @Transactional
    public void markFailed(Long compositionId, String reason) {
        videoCompositionRepository.findById(compositionId).ifPresent(composition -> {
            if (!composition.getStatus().isTerminal()) {
                composition.markFailed(reason);
            }
        });
        heartbeatStore.remove(compositionId);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Long resolveAdvertiserId(Long adVideoId) {
        if (adVideoId == null) return null;
        return adVideoRepository.findAdvertiserIdById(adVideoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "adVideoId에 해당하는 AdVideo를 찾을 수 없습니다. id=" + adVideoId));
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("JSON 직렬화 실패: {}", e.getMessage());
            return value.toString();
        }
    }
}
