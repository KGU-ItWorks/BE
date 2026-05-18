package com.streamly.streamly.domain.videoComposition.service;

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
     * Currently returns the most recent COMPLETED composition regardless of user —
     * this is the stub that will be replaced with preference-based selection once
     * user preference tracking is implemented.
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
     * Stub: ignores userEmail, returns the most recent COMPLETED composition.
     * Replace this method body when preference logic is ready.
     */
    private Optional<VideoComposition> pickCompositionForUser(Long videoId, String userEmail) {
        // TODO: query user preference, filter by objectPrompt, pick best match
        return videoCompositionRepository
                .findFirstByVideoIdAndStatusOrderByRequestedAtDesc(videoId, CompositionStatus.COMPLETED);
    }
}
