package com.streamly.streamly.domain.interaction.service;

import com.streamly.streamly.domain.interaction.dto.FavoritesResponse;
import com.streamly.streamly.domain.interaction.entity.Favorites;
import com.streamly.streamly.domain.interaction.repository.FavoritesRepository;
import com.streamly.streamly.domain.user.entity.Role;
import com.streamly.streamly.domain.user.entity.User;
import com.streamly.streamly.domain.user.repository.UserRepository;
import com.streamly.streamly.domain.video.entity.ApprovalStatus;
import com.streamly.streamly.domain.video.entity.Video;
import com.streamly.streamly.domain.video.entity.VideoStatus;
import com.streamly.streamly.domain.video.repository.VideoRepository;
import com.streamly.streamly.global.exception.user.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("찜 서비스 테스트")
class FavoritesServiceTest {

    @Mock
    private FavoritesRepository favoritesRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VideoRepository videoRepository;

    @InjectMocks
    private FavoritesService favoritesService;

    private User testUser;
    private Video testVideo;
    private Favorites testFavorite;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .nickname("테스트유저")
                .role(Role.ROLE_USER)
                .provider("local")
                .build();

        testVideo = Video.builder()
                .title("테스트 영상")
                .originalFilename("test.mp4")
                .uploader(testUser)
                .status(VideoStatus.COMPLETED)
                .approvalStatus(ApprovalStatus.APPROVED)
                .build();

        testFavorite = Favorites.builder()
                .user(testUser)
                .video(testVideo)
                .build();
    }

    // ==================== getFavoriteVideos ====================

    @Test
    @DisplayName("찜 목록 조회 성공")
    void getFavoriteVideos_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Favorites> favoritesPage = new PageImpl<>(List.of(testFavorite));

        given(userRepository.findByEmail(testUser.getEmail())).willReturn(Optional.of(testUser));
        given(favoritesRepository.findByUser(testUser, pageable)).willReturn(favoritesPage);

        // when
        Page<FavoritesResponse> result = favoritesService.getFavoriteVideos(testUser.getEmail(), pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getVideo().getTitle()).isEqualTo("테스트 영상");

        then(userRepository).should(times(1)).findByEmail(testUser.getEmail());
        then(favoritesRepository).should(times(1)).findByUser(testUser, pageable);
    }

    @Test
    @DisplayName("찜 목록 조회 성공 - 빈 목록")
    void getFavoriteVideos_Success_EmptyList() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Favorites> emptyPage = new PageImpl<>(List.of());

        given(userRepository.findByEmail(testUser.getEmail())).willReturn(Optional.of(testUser));
        given(favoritesRepository.findByUser(testUser, pageable)).willReturn(emptyPage);

        // when
        Page<FavoritesResponse> result = favoritesService.getFavoriteVideos(testUser.getEmail(), pageable);

        // then
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("찜 목록 조회 실패 - 존재하지 않는 사용자")
    void getFavoriteVideos_Fail_UserNotFound() {
        // given
        given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> favoritesService.getFavoriteVideos("unknown@example.com", PageRequest.of(0, 10)))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다.");

        then(favoritesRepository).should(never()).findByUser(any(), any());
    }

    // ==================== toggleFavorite ====================

    @Test
    @DisplayName("찜 추가 성공 - 찜 목록에 없던 영상")
    void toggleFavorite_Add_Success() {
        // given
        given(userRepository.findByEmail(testUser.getEmail())).willReturn(Optional.of(testUser));
        given(videoRepository.findById(1L)).willReturn(Optional.of(testVideo));
        given(favoritesRepository.deleteByUserIdAndVideoId(testUser.getId(), 1L)).willReturn(0);
        given(favoritesRepository.save(any(Favorites.class))).willReturn(testFavorite);

        // when
        boolean result = favoritesService.toggleFavorite(testUser.getEmail(), 1L);

        // then
        assertThat(result).isTrue();
        then(favoritesRepository).should(times(1)).save(any(Favorites.class));
    }

    @Test
    @DisplayName("찜 취소 성공 - 이미 찜한 영상")
    void toggleFavorite_Remove_Success() {
        // given
        given(userRepository.findByEmail(testUser.getEmail())).willReturn(Optional.of(testUser));
        given(videoRepository.findById(1L)).willReturn(Optional.of(testVideo));
        given(favoritesRepository.deleteByUserIdAndVideoId(testUser.getId(), 1L)).willReturn(1);

        // when
        boolean result = favoritesService.toggleFavorite(testUser.getEmail(), 1L);

        // then
        assertThat(result).isFalse();
        then(favoritesRepository).should(never()).save(any(Favorites.class));
    }

    @Test
    @DisplayName("찜 토글 실패 - 존재하지 않는 사용자")
    void toggleFavorite_Fail_UserNotFound() {
        // given
        given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> favoritesService.toggleFavorite("unknown@example.com", 1L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다.");

        then(videoRepository).should(never()).findById(any());
        then(favoritesRepository).should(never()).deleteByUserIdAndVideoId(any(), any());
        then(favoritesRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("찜 토글 실패 - 존재하지 않는 영상")
    void toggleFavorite_Fail_VideoNotFound() {
        // given
        given(userRepository.findByEmail(testUser.getEmail())).willReturn(Optional.of(testUser));
        given(videoRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> favoritesService.toggleFavorite(testUser.getEmail(), 999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("영상을 찾을 수 없습니다.");

        then(favoritesRepository).should(never()).deleteByUserIdAndVideoId(any(), any());
        then(favoritesRepository).should(never()).save(any());
    }
}
