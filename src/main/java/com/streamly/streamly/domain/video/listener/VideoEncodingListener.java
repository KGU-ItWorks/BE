package com.streamly.streamly.domain.video.listener;

import com.streamly.streamly.domain.video.dto.VideoEncodingMessage;
import com.streamly.streamly.domain.video.entity.Video;
import com.streamly.streamly.domain.video.entity.VideoStatus;
import com.streamly.streamly.domain.video.repository.VideoRepository;
import com.streamly.streamly.global.config.RabbitMQConfig;
import com.streamly.streamly.global.service.FFmpegService;
import com.streamly.streamly.global.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoEncodingListener {

    private final VideoRepository videoRepository;
    private final FFmpegService ffmpegService;
    private final S3Service s3Service;

    @Value("${video.encoded.directory}")
    private String encodedDirectory;

    /**
     * RabbitMQ에서 인코딩 메시지를 수신하여 처리
     */
    @RabbitListener(queues = RabbitMQConfig.VIDEO_ENCODING_QUEUE)
    @Transactional
    public void handleVideoEncoding(VideoEncodingMessage message) {
        log.info("Received encoding message for video ID: {}", message.getVideoId());

        Video video = videoRepository.findById(message.getVideoId())
                .orElseThrow(() -> new IllegalArgumentException("Video not found: " + message.getVideoId()));

        try {
            // 1. 상태를 ENCODING으로 변경
            video.updateStatus(VideoStatus.ENCODING);
            video.updateEncodingProgress(0);
            videoRepository.save(video);
            log.info("Video status updated to ENCODING for ID: {}", video.getId());

            // 2. 메타데이터 추출
            FFmpegService.VideoMetadata metadata = ffmpegService.extractMetadata(message.getOriginalFilePath());
            video.updateMetadata(
                    metadata.getDurationSeconds(),
                    metadata.getResolution(),
                    metadata.getVideoCodec(),
                    metadata.getAudioCodec()
            );
            videoRepository.save(video);
            log.info("Metadata extracted for video ID: {}", video.getId());

            // 진행률 업데이트 (메타데이터 추출 완료 시점)
            video.updateEncodingProgress(20);
            videoRepository.save(video);

            // 3. HLS 인코딩 수행
            log.info("Starting HLS encoding for video ID: {}", video.getId());
            String masterPlaylistPath = ffmpegService.encodeToHLS(
                    message.getOriginalFilePath(),
                    message.getOutputDirectory()
            );
            log.info("HLS encoding completed. Master playlist: {}", masterPlaylistPath);

            // 진행률 업데이트 (인코딩 완료 시점)
            video.updateEncodingProgress(60);
            videoRepository.save(video);

            // 4. S3 업로드
            log.info("Starting S3 upload for video ID: {}", video.getId());
            uploadToS3(video, message.getOutputDirectory());

            // 진행률 업데이트 (S3 업로드 완료 시점)
            video.updateEncodingProgress(90);
            videoRepository.save(video);

            // 5. 인코딩 완료 후 상태 업데이트
            video.updateStatus(VideoStatus.COMPLETED);
            video.updateEncodingProgress(100);

            // 6. 개발/테스트 환경: 자동으로 승인 (프로덕션에서는 제거)
            video.approve();

            videoRepository.save(video);
            log.info("Video encoding and S3 upload completed successfully for ID: {} (Auto-approved)", video.getId());

            // 6. 로컬 파일 정리 (옵션)
            // deleteLocalFiles(message.getOriginalFilePath(), message.getOutputDirectory());

        } catch (Exception e) {
            log.error("Video encoding failed for ID: {}", message.getVideoId(), e);

            // 실패 상태로 업데이트
            video.updateStatus(VideoStatus.FAILED);
            videoRepository.save(video);
        }
    }

    /**
     * S3에 인코딩된 파일 업로드
     */
    private void uploadToS3(Video video, String encodedDir) {
        try {
            // S3 키 prefix: videos/{videoId}/
            String s3Prefix = String.format("videos/%d/", video.getId());

            // 인코딩된 디렉토리 전체 업로드
            Path encodedPath = Paths.get(encodedDir);
            int uploadedCount = s3Service.uploadDirectory(encodedPath, s3Prefix);

            log.info("S3 upload completed: {} files uploaded to {}", uploadedCount, s3Prefix);

            // S3 정보 업데이트
            String s3Key = s3Prefix + "master.m3u8";
            String s3Url = s3Service.getUrl(s3Key);
            String cloudfrontUrl = s3Service.getUrl(s3Key); // CloudFront 설정 시 자동으로 CloudFront URL 반환

            video.updateS3Info(s3Key, s3Url, cloudfrontUrl);
            videoRepository.save(video);

            log.info("Video S3 info updated - Key: {}, URL: {}", s3Key, cloudfrontUrl);

        } catch (Exception e) {
            log.error("S3 upload failed for video ID: {}", video.getId(), e);
            throw new RuntimeException("S3 upload failed", e);
        }
    }

    /**
     * 로컬 파일 정리 (인코딩 완료 후)
     */
    private void deleteLocalFiles(String originalFilePath, String encodedDir) {
        try {
            // 원본 파일 삭제
            File originalFile = new File(originalFilePath);
            if (originalFile.exists() && originalFile.delete()) {
                log.info("Original file deleted: {}", originalFilePath);
            }

            // 인코딩된 파일 디렉토리 삭제
            File encodedDirFile = new File(encodedDir);
            if (encodedDirFile.exists()) {
                deleteDirectory(encodedDirFile);
                log.info("Encoded directory deleted: {}", encodedDir);
            }

        } catch (Exception e) {
            log.warn("Failed to delete local files", e);
        }
    }

    /**
     * 디렉토리 재귀 삭제
     */
    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }
}
