package com.streamly.streamly.domain.user.service;

import com.streamly.streamly.domain.user.dto.UploaderRequestDto;
import com.streamly.streamly.domain.user.entity.Role;
import com.streamly.streamly.domain.user.entity.UploaderRequest;
import com.streamly.streamly.domain.user.entity.User;
import com.streamly.streamly.domain.user.repository.UploaderRequestRepository;
import com.streamly.streamly.domain.user.repository.UserRepository;
import com.streamly.streamly.global.exception.user.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 업로더 승급 신청 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UploaderRequestService {
    
    private final UploaderRequestRepository requestRepository;
    private final UserRepository userRepository;
    
    /**
     * 업로더 승급 신청
     */
    @Transactional
    public UploaderRequestDto.Response createRequest(String email, UploaderRequestDto.Create dto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
        
        // 이미 업로더 이상이면 신청 불가
        if (user.getRole() == Role.ROLE_UPLOADER || user.getRole() == Role.ROLE_ADMIN) {
            throw new IllegalStateException("이미 업로더 권한을 가지고 있습니다.");
        }
        
        // 대기 중인 신청이 있는지 확인
        requestRepository.findPendingRequestByUserId(user.getId())
                .ifPresent(r -> {
                    throw new IllegalStateException("이미 대기 중인 신청이 있습니다.");
                });
        
        UploaderRequest request = UploaderRequest.builder()
                .user(user)
                .reason(dto.getReason())
                .build();
        
        UploaderRequest saved = requestRepository.save(request);
        
        log.info("업로더 승급 신청 생성 - userId: {}, requestId: {}", user.getId(), saved.getId());
        
        return UploaderRequestDto.Response.from(saved);
    }
    
    /**
     * 내 신청 내역 조회
     */
    @Transactional(readOnly = true)
    public Page<UploaderRequestDto.Response> getMyRequests(String email, Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
        
        return requestRepository.findByUserId(user.getId(), pageable)
                .map(UploaderRequestDto.Response::from);
    }
    
    /**
     * 모든 신청 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    public Page<UploaderRequestDto.Response> getAllRequests(Pageable pageable) {
        return requestRepository.findAllRequests(pageable)
                .map(UploaderRequestDto.Response::from);
    }
    
    /**
     * 상태별 신청 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    public Page<UploaderRequestDto.Response> getRequestsByStatus(
            UploaderRequest.RequestStatus status, 
            Pageable pageable
    ) {
        return requestRepository.findByStatus(status, pageable)
                .map(UploaderRequestDto.Response::from);
    }
    
    /**
     * 대기 중인 신청 개수
     */
    @Transactional(readOnly = true)
    public long getPendingRequestCount() {
        return requestRepository.countPendingRequests();
    }
    
    /**
     * 신청 승인 (관리자용)
     */
    @Transactional
    public UploaderRequestDto.Response approveRequest(String adminEmail, Long requestId) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new UserNotFoundException("관리자를 찾을 수 없습니다."));
        
        UploaderRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("신청을 찾을 수 없습니다."));
        
        if (request.getStatus() != UploaderRequest.RequestStatus.PENDING) {
            throw new IllegalStateException("이미 처리된 신청입니다.");
        }
        
        // 신청 승인
        request.approve(admin.getId());
        
        // 사용자 권한 업그레이드
        User user = request.getUser();
        user.changeRole(Role.ROLE_UPLOADER);
        
        requestRepository.save(request);
        userRepository.save(user);
        
        log.info("업로더 승급 신청 승인 - requestId: {}, userId: {}, adminId: {}", 
                requestId, user.getId(), admin.getId());
        
        return UploaderRequestDto.Response.from(request);
    }
    
    /**
     * 신청 거부 (관리자용)
     */
    @Transactional
    public UploaderRequestDto.Response rejectRequest(
            String adminEmail, 
            Long requestId, 
            UploaderRequestDto.Reject dto
    ) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new UserNotFoundException("관리자를 찾을 수 없습니다."));
        
        UploaderRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("신청을 찾을 수 없습니다."));
        
        if (request.getStatus() != UploaderRequest.RequestStatus.PENDING) {
            throw new IllegalStateException("이미 처리된 신청입니다.");
        }
        
        // 신청 거부
        request.reject(admin.getId(), dto.getComment());
        requestRepository.save(request);
        
        log.info("업로더 승급 신청 거부 - requestId: {}, userId: {}, adminId: {}", 
                requestId, request.getUser().getId(), admin.getId());
        
        return UploaderRequestDto.Response.from(request);
    }
}
