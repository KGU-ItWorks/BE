package com.streamly.streamly.domain.advertiser.service;

import com.streamly.streamly.domain.advertiser.dto.AdNukiMessage;
import com.streamly.streamly.domain.advertiser.dto.AdVideoDto;
import com.streamly.streamly.domain.advertiser.entity.AdVideo;
import com.streamly.streamly.domain.advertiser.repository.AdVideoRepository;
import com.streamly.streamly.domain.user.entity.User;
import com.streamly.streamly.domain.user.repository.UserRepository;
import com.streamly.streamly.global.config.RabbitMQConfig;
import com.streamly.streamly.global.exception.user.UserNotFoundException;
import com.streamly.streamly.global.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdVideoService {

    private final AdVideoRepository adVideoRepository;
    private final UserRepository userRepository;
    private final FileStorageUtil fileStorageUtil;
    private final RabbitTemplate rabbitTemplate;

    @Value("${ad.video.upload.directory:ad_uploads}")
    private String adUploadDirectory;

    @Value("${ad.callback.url:http://localhost:8080/api/v1/advertiser/callback}")
    private String callbackUrl;

    /**
     * 광고 영상 업로드 - 파일 저장 후 RabbitMQ로 AI 처리 요청
     */
    @Transactional
    public AdVideoDto.Response uploadAdVideo(String email, AdVideoDto.UploadRequest request, MultipartFile videoFile) {
        User advertiser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        validateVideoFile(videoFile);

        String savedFilePath = fileStorageUtil.storeFile(videoFile);

        AdVideo adVideo = AdVideo.builder()
                .advertiser(advertiser)
                .title(request.getTitle())
                .description(request.getDescription())
                .originalFilename(videoFile.getOriginalFilename())
                .originalFileSize(videoFile.getSize())
                .filePath(savedFilePath)
                .build();

        AdVideo saved = adVideoRepository.save(adVideo);

        // AI 서버에 누끼 처리 요청 (RabbitMQ 비동기)
        AdNukiMessage message = AdNukiMessage.builder()
                .adVideoId(saved.getId())
                .filePath(savedFilePath)
                .callbackUrl(callbackUrl)
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.AD_NUKI_EXCHANGE,
                RabbitMQConfig.AD_NUKI_ROUTING_KEY,
                message
        );

        log.info("광고 영상 업로드 완료 및 누끼 처리 요청 전송 - adVideoId: {}, advertiser: {}", saved.getId(), email);

        return AdVideoDto.Response.from(saved);
    }

    /**
     * 내 광고 영상 목록 조회 (광고주 본인만)
     */
    @Transactional(readOnly = true)
    public Page<AdVideoDto.Response> getMyAdVideos(String email, Pageable pageable) {
        User advertiser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        return adVideoRepository.findByAdvertiserId(advertiser.getId(), pageable)
                .map(AdVideoDto.Response::from);
    }

    /**
     * 내 광고 영상 상세 조회 (본인 소유 검증 포함)
     */
    @Transactional(readOnly = true)
    public AdVideoDto.Response getMyAdVideo(String email, Long adVideoId) {
        User advertiser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        AdVideo adVideo = adVideoRepository.findById(adVideoId)
                .orElseThrow(() -> new IllegalArgumentException("광고 영상을 찾을 수 없습니다."));

        validateOwnership(adVideo, advertiser.getId());

        return AdVideoDto.Response.from(adVideo);
    }

    /**
     * 내 광고 영상 삭제 - 파일 및 누끼 결과 모두 삭제 (본인 소유 검증 포함)
     */
    @Transactional
    public void deleteMyAdVideo(String email, Long adVideoId) {
        User advertiser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        AdVideo adVideo = adVideoRepository.findById(adVideoId)
                .orElseThrow(() -> new IllegalArgumentException("광고 영상을 찾을 수 없습니다."));

        validateOwnership(adVideo, advertiser.getId());

        deleteAdVideoFiles(adVideo);
        adVideoRepository.delete(adVideo);

        log.info("광고 영상 삭제 완료 - adVideoId: {}, advertiser: {}", adVideoId, email);
    }

    /**
     * AI 콜백 수신 - 누끼 처리 결과 반영
     */
    @Transactional
    public void handleNukiCallback(AdVideoDto.NukiCallbackRequest callback) {
        AdVideo adVideo = adVideoRepository.findById(callback.getAdVideoId())
                .orElseThrow(() -> new IllegalArgumentException("광고 영상을 찾을 수 없습니다."));

        if (callback.isSuccess()) {
            adVideo.markDone(callback.getNukiDirPath());
            log.info("누끼 처리 완료 - adVideoId: {}, nukiDirPath: {}", callback.getAdVideoId(), callback.getNukiDirPath());
        } else {
            adVideo.markFailed(callback.getFailReason());
            log.warn("누끼 처리 실패 - adVideoId: {}, reason: {}", callback.getAdVideoId(), callback.getFailReason());
        }

        adVideoRepository.save(adVideo);
    }

    private void validateOwnership(AdVideo adVideo, Long userId) {
        if (!adVideo.getAdvertiser().getId().equals(userId)) {
            throw new IllegalArgumentException("해당 광고 영상에 대한 권한이 없습니다.");
        }
    }

    private void validateVideoFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }
        if (!fileStorageUtil.isVideoFile(file)) {
            throw new IllegalArgumentException("영상 파일만 업로드할 수 있습니다.");
        }
    }

    private void deleteAdVideoFiles(AdVideo adVideo) {
        // 원본 영상 삭제
        try {
            if (adVideo.getFilePath() != null) {
                fileStorageUtil.deleteFile(adVideo.getFilePath());
                log.info("광고 원본 파일 삭제 완료: {}", adVideo.getFilePath());
            }
        } catch (Exception e) {
            log.warn("광고 원본 파일 삭제 실패 (계속 진행): {}", adVideo.getFilePath(), e);
        }

        // 누끼 결과 디렉토리 삭제
        try {
            if (adVideo.getNukiDirPath() != null) {
                Path nukiDir = Paths.get(adVideo.getNukiDirPath());
                if (Files.exists(nukiDir)) {
                    try (Stream<Path> paths = Files.walk(nukiDir)) {
                        paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                            try { Files.delete(p); } catch (Exception ex) {
                                log.warn("누끼 파일 삭제 실패: {}", p, ex);
                            }
                        });
                    }
                    log.info("누끼 결과 디렉토리 삭제 완료: {}", adVideo.getNukiDirPath());
                }
            }
        } catch (Exception e) {
            log.warn("누끼 결과 디렉토리 삭제 실패 (계속 진행): {}", adVideo.getNukiDirPath(), e);
        }
    }
}
