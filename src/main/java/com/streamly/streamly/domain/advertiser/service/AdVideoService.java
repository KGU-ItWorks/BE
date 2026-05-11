package com.streamly.streamly.domain.advertiser.service;

import com.streamly.streamly.domain.advertiser.dto.AdNukiMessage;
import com.streamly.streamly.domain.advertiser.dto.AdVideoDto;
import com.streamly.streamly.domain.advertiser.entity.AdObjectCategory;
import com.streamly.streamly.domain.advertiser.entity.AdVideo;
import com.streamly.streamly.domain.advertiser.repository.AdVideoRepository;
import com.streamly.streamly.domain.user.entity.User;
import com.streamly.streamly.domain.user.repository.UserRepository;
import com.streamly.streamly.global.config.RabbitMQConfig;
import com.streamly.streamly.global.exception.user.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdVideoService {

    private final AdVideoRepository adVideoRepository;
    private final UserRepository userRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${ad.video.upload.directory:C:/ItWorks/uploads/advideos}")
    private String adUploadDirectory;

    @Value("${ad.callback.url:http://localhost:8080/api/v1/advertiser/callback/nuki}")
    private String callbackUrl;

    @Value("${ad.nuki.directory:C:/ItWorks/nuki_results}")
    private String nukiDirectory;

    @Value("${server.url:http://localhost:8080}")
    private String serverUrl;

    /**
     * 광고 영상 업로드 - C:/ItWorks/uploads/advideos/{uuid}/{원본파일명} 저장
     */
    @Transactional
    public AdVideoDto.Response uploadAdVideo(String email, AdVideoDto.UploadRequest request, MultipartFile videoFile) {
        User advertiser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        validateVideoFile(videoFile);

        AdObjectCategory category = parseCategory(request.getObjectCategory());
        String savedFilePath = storeAdVideo(videoFile);

        AdVideo adVideo = AdVideo.builder()
                .advertiser(advertiser)
                .title(request.getTitle())
                .description(request.getDescription())
                .objectCategory(category)
                .originalFilename(videoFile.getOriginalFilename())
                .originalFileSize(videoFile.getSize())
                .filePath(savedFilePath)
                .build();

        AdVideo saved = adVideoRepository.save(adVideo);

        String objectPrompt = category != null ? category.toSam3Prompt() : "object";

        // AI 서버에 누끼 처리 요청 (RabbitMQ 비동기)
        AdNukiMessage message = AdNukiMessage.builder()
                .adVideoId(saved.getId())
                .filePath(savedFilePath)
                .callbackUrl(callbackUrl)
                .objectPrompt(objectPrompt)
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.AD_NUKI_EXCHANGE,
                RabbitMQConfig.AD_NUKI_ROUTING_KEY,
                message
        );

        log.info("광고 영상 업로드 완료 - adVideoId: {}, path: {}, advertiser: {}",
                saved.getId(), savedFilePath, email);

        return AdVideoDto.Response.from(saved);
    }

    /**
     * 광고 영상 파일 저장
     * 저장 구조: {adUploadDirectory}/{uuid}/{원본파일명}
     */
    private String storeAdVideo(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("파일명이 유효하지 않습니다.");
        }

        String uuid = UUID.randomUUID().toString();
        Path targetDir = Paths.get(adUploadDirectory, uuid);

        try {
            Files.createDirectories(targetDir);
            Path targetPath = targetDir.resolve(originalFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("광고 영상 저장 완료: {}", targetPath);
            return targetPath.toString();
        } catch (IOException e) {
            log.error("광고 영상 저장 실패: {}", originalFilename, e);
            throw new RuntimeException("광고 영상 파일을 저장할 수 없습니다.", e);
        }
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
            log.info("누끼 처리 완료 - adVideoId: {}, nukiDirPath: {}",
                    callback.getAdVideoId(), callback.getNukiDirPath());
        } else {
            adVideo.markFailed(callback.getFailReason());
            log.warn("누끼 처리 실패 - adVideoId: {}, reason: {}",
                    callback.getAdVideoId(), callback.getFailReason());
        }

        adVideoRepository.save(adVideo);
    }

    /**
     * 누끼 이미지 목록 조회 - nukiDirPath 내 PNG 파일을 URL로 변환해 반환
     */
    @Transactional(readOnly = true)
    public AdVideoDto.NukiImagesResponse getNukiImages(String email, Long adVideoId) {
        User advertiser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        AdVideo adVideo = adVideoRepository.findById(adVideoId)
                .orElseThrow(() -> new IllegalArgumentException("광고 영상을 찾을 수 없습니다."));

        validateOwnership(adVideo, advertiser.getId());

        if (adVideo.getNukiDirPath() == null) {
            return AdVideoDto.NukiImagesResponse.builder()
                    .adVideoId(adVideoId)
                    .status(adVideo.getStatus().name())
                    .imageUrls(List.of())
                    .build();
        }

        List<String> imageUrls;
        try (Stream<Path> files = Files.list(Paths.get(adVideo.getNukiDirPath()))) {
            imageUrls = files
                    .filter(p -> p.toString().toLowerCase().endsWith(".png"))
                    .sorted()
                    .map(p -> serverUrl + "/nuki/" + adVideoId + "/" + p.getFileName().toString())
                    .toList();
        } catch (IOException e) {
            log.warn("누끼 이미지 디렉토리 읽기 실패 - adVideoId: {}, path: {}", adVideoId, adVideo.getNukiDirPath(), e);
            imageUrls = List.of();
        }

        return AdVideoDto.NukiImagesResponse.builder()
                .adVideoId(adVideoId)
                .status(adVideo.getStatus().name())
                .imageUrls(imageUrls)
                .build();
    }

    private AdObjectCategory parseCategory(String categoryStr) {
        if (categoryStr == null || categoryStr.isBlank()) return null;
        try {
            return AdObjectCategory.valueOf(categoryStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("알 수 없는 카테고리: {}", categoryStr);
            return null;
        }
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
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("video/") && !contentType.equals("application/octet-stream"))) {
            throw new IllegalArgumentException("영상 파일만 업로드할 수 있습니다.");
        }
    }

    /**
     * 광고 영상 관련 파일 전체 삭제 (원본 + 누끼 결과)
     * 저장 구조가 {adUploadDirectory}/{uuid}/{파일명} 이므로 uuid 디렉토리째 삭제
     */
    private void deleteAdVideoFiles(AdVideo adVideo) {
        // 원본 영상 디렉토리 삭제 ({adUploadDirectory}/{uuid}/ 전체)
        try {
            if (adVideo.getFilePath() != null) {
                Path fileDir = Paths.get(adVideo.getFilePath()).getParent();
                deleteDirectory(fileDir);
                log.info("광고 영상 디렉토리 삭제 완료: {}", fileDir);
            }
        } catch (Exception e) {
            log.warn("광고 영상 디렉토리 삭제 실패 (계속 진행): {}", adVideo.getFilePath(), e);
        }

        // 누끼 결과 디렉토리 삭제 (C:/ItWorks/nuki_results/{adVideoId}/)
        try {
            if (adVideo.getNukiDirPath() != null) {
                deleteDirectory(Paths.get(adVideo.getNukiDirPath()));
                log.info("누끼 결과 디렉토리 삭제 완료: {}", adVideo.getNukiDirPath());
            }
        } catch (Exception e) {
            log.warn("누끼 결과 디렉토리 삭제 실패 (계속 진행): {}", adVideo.getNukiDirPath(), e);
        }
    }

    private void deleteDirectory(Path dir) throws IOException {
        if (dir != null && Files.exists(dir)) {
            try (Stream<Path> paths = Files.walk(dir)) {
                paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try { Files.delete(p); } catch (Exception ex) {
                        log.warn("파일 삭제 실패: {}", p, ex);
                    }
                });
            }
        }
    }
}
