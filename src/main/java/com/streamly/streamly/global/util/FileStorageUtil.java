package com.streamly.streamly.global.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 파일 저장 유틸리티
 * 업로드된 파일을 로컬 디스크에 임시 저장
 */
@Slf4j
@Component
public class FileStorageUtil {

    private final Path uploadLocation;

    public FileStorageUtil(@Value("${file.upload-dir:./uploads}") String uploadDir) {
        this.uploadLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        
        try {
            Files.createDirectories(this.uploadLocation);
            log.info("파일 업로드 디렉토리 생성: {}", this.uploadLocation);
        } catch (IOException e) {
            throw new RuntimeException("파일 업로드 디렉토리를 생성할 수 없습니다.", e);
        }
    }

    /**
     * 파일 저장
     * @param file 업로드된 파일
     * @return 저장된 파일 경로
     */
    public String storeFile(MultipartFile file) {
        return storeFile(file, null);
    }

    /**
     * 파일 저장 (서브디렉토리 지원)
     * @param file 업로드된 파일
     * @param subDirectory 서브디렉토리 (예: "thumbnails")
     * @return 저장된 파일 경로
     */
    public String storeFile(MultipartFile file, String subDirectory) {
        // 원본 파일명
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("파일명이 유효하지 않습니다.");
        }

        // 고유한 파일명 생성 (UUID + 확장자)
        String extension = getFileExtension(originalFilename);
        String storedFilename = UUID.randomUUID().toString() + extension;

        try {
            // 서브디렉토리 처리
            Path targetDirectory = this.uploadLocation;
            if (subDirectory != null && !subDirectory.isEmpty()) {
                targetDirectory = this.uploadLocation.resolve(subDirectory);
                Files.createDirectories(targetDirectory);
            }

            // 파일 저장
            Path targetLocation = targetDirectory.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            log.info("파일 저장 완료: {} -> {}", originalFilename, storedFilename);
            return targetLocation.toString();
        } catch (IOException e) {
            log.error("파일 저장 실패: {}", originalFilename, e);
            throw new RuntimeException("파일을 저장할 수 없습니다.", e);
        }
    }

    /**
     * 파일 삭제
     * @param filePath 파일 경로
     */
    public void deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            Files.deleteIfExists(path);
            log.info("파일 삭제 완료: {}", filePath);
        } catch (IOException e) {
            log.error("파일 삭제 실패: {}", filePath, e);
        }
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String filename) {
        int lastIndexOf = filename.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return "";
        }
        return filename.substring(lastIndexOf);
    }

    /**
     * 파일 타입 검증 (영상 파일만 허용)
     */
    public boolean isVideoFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) {
            return false;
        }
        
        return contentType.startsWith("video/") || 
               contentType.equals("application/octet-stream"); // 일부 브라우저에서 사용
    }

    /**
     * 파일 크기 검증
     * @param file 파일
     * @param maxSizeMB 최대 크기 (MB)
     */
    public boolean isValidFileSize(MultipartFile file, long maxSizeMB) {
        long maxSizeBytes = maxSizeMB * 1024 * 1024;
        return file.getSize() <= maxSizeBytes;
    }
}
