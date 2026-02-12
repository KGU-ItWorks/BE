package com.streamly.streamly.domain.video.service;

import com.streamly.streamly.domain.user.entity.User;
import com.streamly.streamly.domain.video.dto.VideoEncodingMessage;
import com.streamly.streamly.domain.video.dto.VideoUploadRequest;
import com.streamly.streamly.domain.video.dto.VideoUploadResponse;
import com.streamly.streamly.domain.video.entity.Video;
import com.streamly.streamly.domain.video.entity.VideoStatus;
import com.streamly.streamly.domain.video.repository.VideoRepository;
import com.streamly.streamly.global.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoUploadService {

    private final VideoRepository videoRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${video.upload.directory:uploads}")
    private String uploadDirectory;

    @Value("${video.encoded.directory:encoded}")
    private String encodedDirectory;

    /**
     * 영상 업로드 처리
     */
    @Transactional
    public VideoUploadResponse uploadVideo(
            VideoUploadRequest request,
            MultipartFile videoFile,
            MultipartFile thumbnailFile,
            User uploader
    ) throws IOException {
        log.info("Starting video upload for user: {}", uploader.getEmail());

        // 1. 파일 유효성 검증
        validateVideoFile(videoFile);
        
        if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
            validateThumbnailFile(thumbnailFile);
        }

        // 2. 원본 영상 파일 저장
        String originalFileName = videoFile.getOriginalFilename();
        String uniqueFileName = UUID.randomUUID().toString() + getFileExtension(originalFileName);
        Path uploadPath = Paths.get(uploadDirectory);
        
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        Path videoFilePath = uploadPath.resolve(uniqueFileName);
        Files.copy(videoFile.getInputStream(), videoFilePath, StandardCopyOption.REPLACE_EXISTING);
        
        log.info("Video file saved: {}", videoFilePath);

        // 3. 썸네일 파일 저장 (있는 경우)
        String thumbnailUrl = null;
        if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
            thumbnailUrl = saveThumbnail(thumbnailFile);
            log.info("Thumbnail saved: {}", thumbnailUrl);
        }

        // 4. Video 엔티티 생성 및 저장
        Video video = Video.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .ageRating(request.getAgeRating())
                .uploader(uploader)
                .originalFilename(originalFileName)
                .originalFileSize(videoFile.getSize())
                .originalFilePath(videoFilePath.toString())
                .thumbnailUrl(thumbnailUrl)
                .status(VideoStatus.UPLOADED)
                .build();

        video = videoRepository.save(video);
        log.info("Video entity created with ID: {}", video.getId());

        // 5. 인코딩 작업을 RabbitMQ에 전송
        String outputDir = encodedDirectory + "/" + video.getId();
        VideoEncodingMessage message = VideoEncodingMessage.builder()
                .videoId(video.getId())
                .originalFilePath(videoFilePath.toString())
                .outputDirectory(outputDir)
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.VIDEO_ENCODING_EXCHANGE,
                RabbitMQConfig.VIDEO_ENCODING_ROUTING_KEY,
                message
        );
        
        log.info("Encoding message sent to RabbitMQ for video ID: {}", video.getId());

        // 6. 응답 반환
        return VideoUploadResponse.builder()
                .videoId(video.getId())
                .title(video.getTitle())
                .status(video.getStatus())
                .message("영상이 업로드되었습니다. 인코딩이 진행됩니다.")
                .build();
    }

    /**
     * 영상 파일 유효성 검증
     */
    private void validateVideoFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("영상 파일이 필요합니다.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw new IllegalArgumentException("유효한 영상 파일이 아닙니다.");
        }

        // 파일 크기 제한 (예: 500MB)
        long maxSize = 500 * 1024 * 1024; // 500MB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("영상 파일 크기는 500MB를 초과할 수 없습니다.");
        }
    }

    /**
     * 썸네일 파일 유효성 검증
     */
    private void validateThumbnailFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("유효한 이미지 파일이 아닙니다.");
        }

        // 파일 크기 제한 (예: 5MB)
        long maxSize = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("썸네일 파일 크기는 5MB를 초과할 수 없습니다.");
        }
    }

    /**
     * 썸네일 파일 저장
     */
    private String saveThumbnail(MultipartFile thumbnailFile) throws IOException {
        String originalFileName = thumbnailFile.getOriginalFilename();
        String uniqueFileName = UUID.randomUUID().toString() + getFileExtension(originalFileName);
        
        Path thumbnailPath = Paths.get(uploadDirectory, "thumbnails");
        if (!Files.exists(thumbnailPath)) {
            Files.createDirectories(thumbnailPath);
        }
        
        Path filePath = thumbnailPath.resolve(uniqueFileName);
        Files.copy(thumbnailFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        // 상대 경로 또는 URL 반환 (추후 S3 URL로 변경될 수 있음)
        return "/thumbnails/" + uniqueFileName;
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
