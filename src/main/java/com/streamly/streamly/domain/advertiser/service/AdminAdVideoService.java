package com.streamly.streamly.domain.advertiser.service;

import com.streamly.streamly.domain.advertiser.dto.AdVideoDto;
import com.streamly.streamly.domain.advertiser.entity.AdVideo;
import com.streamly.streamly.domain.advertiser.entity.AdVideoStatus;
import com.streamly.streamly.domain.advertiser.repository.AdVideoRepository;
import com.streamly.streamly.global.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAdVideoService {

    private final AdVideoRepository adVideoRepository;
    private final FileStorageUtil fileStorageUtil;

    /**
     * 전체 광고 영상 목록 조회 (관리자 - 모든 광고주 포함)
     */
    @Transactional(readOnly = true)
    public Page<AdVideoDto.Response> getAllAdVideos(String status, Pageable pageable) {
        if (status != null) {
            AdVideoStatus adVideoStatus = AdVideoStatus.valueOf(status);
            return adVideoRepository.findByStatus(adVideoStatus, pageable)
                    .map(AdVideoDto.Response::from);
        }
        return adVideoRepository.findAllWithAdvertiser(pageable)
                .map(AdVideoDto.Response::from);
    }

    /**
     * 광고 영상 강제 삭제 (관리자)
     */
    @Transactional
    public void forceDeleteAdVideo(Long adVideoId) {
        AdVideo adVideo = adVideoRepository.findById(adVideoId)
                .orElseThrow(() -> new IllegalArgumentException("광고 영상을 찾을 수 없습니다."));

        log.info("광고 영상 강제 삭제 시작 - adVideoId: {}, advertiser: {}",
                adVideoId, adVideo.getAdvertiser().getEmail());

        deleteFiles(adVideo);
        adVideoRepository.delete(adVideo);

        log.info("광고 영상 강제 삭제 완료 - adVideoId: {}", adVideoId);
    }

    /**
     * 대시보드 통계 (관리자)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAdVideos", adVideoRepository.count());
        stats.put("pendingCount", adVideoRepository.countByStatus(AdVideoStatus.PENDING));
        stats.put("processingCount", adVideoRepository.countByStatus(AdVideoStatus.PROCESSING));
        stats.put("doneCount", adVideoRepository.countByStatus(AdVideoStatus.DONE));
        stats.put("failedCount", adVideoRepository.countByStatus(AdVideoStatus.FAILED));
        return stats;
    }

    private void deleteFiles(AdVideo adVideo) {
        try {
            if (adVideo.getFilePath() != null) {
                fileStorageUtil.deleteFile(adVideo.getFilePath());
            }
        } catch (Exception e) {
            log.warn("광고 원본 파일 삭제 실패 (계속 진행): {}", adVideo.getFilePath(), e);
        }

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
                }
            }
        } catch (Exception e) {
            log.warn("누끼 결과 디렉토리 삭제 실패 (계속 진행): {}", adVideo.getNukiDirPath(), e);
        }
    }
}
