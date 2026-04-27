package com.streamly.streamly.domain.advertiser.service;

import com.streamly.streamly.domain.advertiser.dto.AdvertiserRequestDto;
import com.streamly.streamly.domain.advertiser.entity.AdvertiserRequest;
import com.streamly.streamly.domain.advertiser.repository.AdvertiserRequestRepository;
import com.streamly.streamly.domain.user.entity.Role;
import com.streamly.streamly.domain.user.entity.User;
import com.streamly.streamly.domain.user.repository.UserRepository;
import com.streamly.streamly.global.exception.user.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdvertiserRequestService {

    private final AdvertiserRequestRepository requestRepository;
    private final UserRepository userRepository;

    /**
     * 광고주 신청
     */
    @Transactional
    public AdvertiserRequestDto.Response createRequest(String email, AdvertiserRequestDto.Create dto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        if (user.getRole() == Role.ROLE_ADVERTISER || user.getRole() == Role.ROLE_ADMIN) {
            throw new IllegalStateException("이미 광고주 이상의 권한을 가지고 있습니다.");
        }

        requestRepository.findPendingRequestByUserId(user.getId())
                .ifPresent(r -> { throw new IllegalStateException("이미 대기 중인 광고주 신청이 있습니다."); });

        AdvertiserRequest request = AdvertiserRequest.builder()
                .user(user)
                .reason(dto.getReason())
                .build();

        AdvertiserRequest saved = requestRepository.save(request);
        log.info("광고주 신청 생성 - userId: {}, requestId: {}", user.getId(), saved.getId());

        return AdvertiserRequestDto.Response.from(saved);
    }

    /**
     * 내 신청 내역 조회
     */
    @Transactional(readOnly = true)
    public Page<AdvertiserRequestDto.Response> getMyRequests(String email, Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        return requestRepository.findByUserId(user.getId(), pageable)
                .map(AdvertiserRequestDto.Response::from);
    }

    /**
     * 전체 신청 조회 (관리자)
     */
    @Transactional(readOnly = true)
    public Page<AdvertiserRequestDto.Response> getAllRequests(Pageable pageable) {
        return requestRepository.findAllRequests(pageable)
                .map(AdvertiserRequestDto.Response::from);
    }

    /**
     * 상태별 신청 조회 (관리자)
     */
    @Transactional(readOnly = true)
    public Page<AdvertiserRequestDto.Response> getRequestsByStatus(
            AdvertiserRequest.RequestStatus status, Pageable pageable) {
        return requestRepository.findByStatus(status, pageable)
                .map(AdvertiserRequestDto.Response::from);
    }

    /**
     * 신청 승인 (관리자)
     */
    @Transactional
    public AdvertiserRequestDto.Response approveRequest(String adminEmail, Long requestId) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new UserNotFoundException("관리자를 찾을 수 없습니다."));

        AdvertiserRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("신청을 찾을 수 없습니다."));

        if (request.getStatus() != AdvertiserRequest.RequestStatus.PENDING) {
            throw new IllegalStateException("이미 처리된 신청입니다.");
        }

        request.approve(admin.getId());

        User user = request.getUser();
        user.changeRole(Role.ROLE_ADVERTISER);

        requestRepository.save(request);
        userRepository.save(user);

        log.info("광고주 신청 승인 - requestId: {}, userId: {}, adminId: {}",
                requestId, user.getId(), admin.getId());

        return AdvertiserRequestDto.Response.from(request);
    }

    /**
     * 신청 거부 (관리자)
     */
    @Transactional
    public AdvertiserRequestDto.Response rejectRequest(
            String adminEmail, Long requestId, AdvertiserRequestDto.Reject dto) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new UserNotFoundException("관리자를 찾을 수 없습니다."));

        AdvertiserRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("신청을 찾을 수 없습니다."));

        if (request.getStatus() != AdvertiserRequest.RequestStatus.PENDING) {
            throw new IllegalStateException("이미 처리된 신청입니다.");
        }

        request.reject(admin.getId(), dto.getComment());
        requestRepository.save(request);

        log.info("광고주 신청 거부 - requestId: {}, userId: {}", requestId, request.getUser().getId());

        return AdvertiserRequestDto.Response.from(request);
    }

    public long getPendingRequestCount() {
        return requestRepository.countPendingRequests();
    }
}
