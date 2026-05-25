package com.streamly.streamly.domain.videoComposition.service;

import com.streamly.streamly.domain.videoComposition.entity.AdvertiserApprovalStatus;
import com.streamly.streamly.domain.videoComposition.entity.CompositionStatus;
import com.streamly.streamly.domain.videoComposition.entity.VideoComposition;
import com.streamly.streamly.domain.videoComposition.repository.VideoCompositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaylistResolverService {

    private final VideoCompositionRepository videoCompositionRepository;

    /**
     * Resolve the HLS master playlist path for a given user watching a video.
     *
     * TODO: look up userEmail's ad preference (brand/category) and pick the
     *       composition whose objectPrompt matches, falling back to most recent.
     *
     * @return relative playlist path e.g. "/encoded/15/versions/<taskId>/master.m3u8",
     *         or the base path "/encoded/{videoId}/master.m3u8" if no composition exists.
     */
    @Transactional(readOnly = true)
    public String resolve(Long videoId, String userEmail) {
        Optional<VideoComposition> composition = pickCompositionForUser(videoId, userEmail);

        if (composition.isPresent()) {
            String taskId = composition.get().getTaskId();
            String path = "/encoded/" + videoId + "/versions/" + taskId + "/master.m3u8";
            log.debug("playlist resolved - videoId: {}, user: {}, taskId: {}", videoId, userEmail, taskId);
            return path;
        }

        // No composition available — serve the original
        return "/encoded/" + videoId + "/master.m3u8";
    }

    /**
     * Pick which composition to show this user.
     * Only returns compositions the advertiser has explicitly APPROVED —
     * PENDING and REJECTED ones are invisible to normal viewers.
     */
    private Optional<VideoComposition> pickCompositionForUser(Long videoId, String userEmail) {
        // TODO: query user preference, filter by objectPrompt, pick best match
        return videoCompositionRepository
                .findFirstByVideoIdAndStatusAndApprovalStatusOrderByPublishedAtDesc(
                        videoId, CompositionStatus.COMPLETED, AdvertiserApprovalStatus.APPROVED);
    }
}
