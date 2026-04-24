package com.streamly.streamly.domain.video.listener;

import com.streamly.streamly.domain.video.dto.VideoEncodingMessage;
import com.streamly.streamly.domain.video.entity.Video;
import com.streamly.streamly.domain.video.entity.VideoStatus;
import com.streamly.streamly.domain.video.repository.VideoRepository;
import com.streamly.streamly.global.config.RabbitMQConfig;
import com.streamly.streamly.global.service.FFmpegService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Paths;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoEncodingListener {

    private final VideoRepository videoRepository;
    private final FFmpegService ffmpegService;

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

            // 4. 로컬 경로 정보를 DB에 저장
            log.info("Saving local encoded path for video ID: {}", video.getId());
            saveLocalEncodedInfo(video, message.getOutputDirectory());

            // 진행률 업데이트 (저장 완료 시점)
            video.updateEncodingProgress(90);
            videoRepository.save(video);

            // 5. 인코딩 완료 후 상태 업데이트
            video.updateStatus(VideoStatus.COMPLETED);
            video.updateEncodingProgress(100);

            // 6. 승인 상태는 PENDING으로 유지 (관리자가 승인해야 함)
            videoRepository.save(video);
            log.info("Video encoding completed and saved locally for ID: {} (Waiting for admin approval)", video.getId());

            // 7. 원본 파일만 삭제 (인코딩 파일은 로컬에 유지)
            deleteOriginalFile(message.getOriginalFilePath());

        } catch (Exception e) {
            log.error("Video encoding failed for ID: {}", message.getVideoId(), e);

            // 실패 상태로 업데이트
            video.updateStatus(VideoStatus.FAILED);
            videoRepository.save(video);
        }
    }

    /**
     * 로컬 인코딩 경로 정보를 DB에 저장
     */
    private void saveLocalEncodedInfo(Video video, String encodedDir) {
        String masterPlaylistPath = encodedDir + "/master.m3u8";
        // 로컬 경로를 서빙 가능한 URL로 변환: encoded/{videoId}/master.m3u8
        String localUrl = "/encoded/" + video.getId() + "/master.m3u8";

        video.updateS3Info(masterPlaylistPath, localUrl, localUrl);
        videoRepository.save(video);

        log.info("Local encoded info saved - Path: {}, URL: {}", masterPlaylistPath, localUrl);
    }

    /**
     * 원본 파일만 삭제 (인코딩 파일은 로컬에 유지)
     */
    private void deleteOriginalFile(String originalFilePath) {
        try {
            File originalFile = new File(originalFilePath);
            if (originalFile.exists() && originalFile.delete()) {
                log.info("Original file deleted: {}", originalFilePath);
            }
        } catch (Exception e) {
            log.warn("Failed to delete original file: {}", originalFilePath, e);
        }
    }
}
