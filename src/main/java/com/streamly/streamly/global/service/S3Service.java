package com.streamly.streamly.global.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * AWS S3 파일 업로드/다운로드 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    @Value("${aws.cloudfront.domain:}")
    private String cloudfrontDomain;

    /**
     * 단일 파일 업로드
     *
     * @param file    업로드할 파일
     * @param s3Key   S3 키 (경로 + 파일명)
     * @return S3 URL
     */
    public String uploadFile(File file, String s3Key) {
        try {
            String contentType = determineContentType(file);

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));

            log.info("S3 업로드 성공: {}", s3Key);

            // CloudFront URL 반환 (설정되어 있으면)
            if (cloudfrontDomain != null && !cloudfrontDomain.isEmpty()) {
                return String.format("https://%s/%s", cloudfrontDomain, s3Key);
            }

            // S3 직접 URL 반환
            return String.format("https://%s.s3.%s.amazonaws.com/%s",
                    bucketName, region, s3Key);

        } catch (Exception e) {
            log.error("S3 업로드 실패: {}", s3Key, e);
            throw new RuntimeException("S3 업로드 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 디렉토리 전체 업로드 (재귀적)
     *
     * @param localDir 로컬 디렉토리
     * @param s3Prefix S3 prefix (예: videos/123/)
     * @return 업로드된 파일 수
     */
    public int uploadDirectory(Path localDir, String s3Prefix) {
        try {
            List<Path> files = new ArrayList<>();

            // 디렉토리 내 모든 파일 수집 (재귀적)
            try (Stream<Path> paths = Files.walk(localDir)) {
                paths.filter(Files::isRegularFile)
                        .forEach(files::add);
            }

            log.info("디렉토리 업로드 시작: {} ({} 개 파일)", localDir, files.size());

            int successCount = 0;
            for (Path filePath : files) {
                try {
                    // 상대 경로 계산
                    Path relativePath = localDir.relativize(filePath);
                    String s3Key = s3Prefix + relativePath.toString().replace("\\", "/");

                    // 파일 업로드
                    uploadFile(filePath.toFile(), s3Key);
                    successCount++;

                } catch (Exception e) {
                    log.error("파일 업로드 실패: {}", filePath, e);
                }
            }

            log.info("디렉토리 업로드 완료: {}/{} 개 파일", successCount, files.size());
            return successCount;

        } catch (IOException e) {
            log.error("디렉토리 스캔 실패: {}", localDir, e);
            throw new RuntimeException("디렉토리 업로드 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 파일 삭제
     *
     * @param s3Key S3 키
     */
    public void deleteFile(String s3Key) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("S3 파일 삭제 완료: {}", s3Key);

        } catch (Exception e) {
            log.error("S3 파일 삭제 실패: {}", s3Key, e);
            throw new RuntimeException("S3 파일 삭제 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 디렉토리 전체 삭제
     *
     * @param s3Prefix S3 prefix
     */
    public void deleteDirectory(String s3Prefix) {
        try {
            // prefix로 시작하는 모든 객체 조회
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(s3Prefix)
                    .build();

            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);

            if (listResponse.contents().isEmpty()) {
                log.info("삭제할 파일 없음: {}", s3Prefix);
                return;
            }

            // 모든 객체 삭제
            List<ObjectIdentifier> objectsToDelete = listResponse.contents().stream()
                    .map(s3Object -> ObjectIdentifier.builder()
                            .key(s3Object.key())
                            .build())
                    .toList();

            DeleteObjectsRequest deleteRequest = DeleteObjectsRequest.builder()
                    .bucket(bucketName)
                    .delete(Delete.builder().objects(objectsToDelete).build())
                    .build();

            s3Client.deleteObjects(deleteRequest);

            log.info("S3 디렉토리 삭제 완료: {} ({} 개 파일)", s3Prefix, objectsToDelete.size());

        } catch (Exception e) {
            log.error("S3 디렉토리 삭제 실패: {}", s3Prefix, e);
            throw new RuntimeException("S3 디렉토리 삭제 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 파일 존재 여부 확인
     *
     * @param s3Key S3 키
     * @return 존재 여부
     */
    public boolean fileExists(String s3Key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.headObject(headObjectRequest);
            return true;

        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.error("S3 파일 존재 확인 실패: {}", s3Key, e);
            throw new RuntimeException("S3 파일 존재 확인 실패: " + e.getMessage(), e);
        }
    }

    /**
     * CloudFront URL 생성
     *
     * @param s3Key S3 키
     * @return CloudFront URL 또는 S3 URL
     */
    public String getUrl(String s3Key) {
        if (cloudfrontDomain != null && !cloudfrontDomain.isEmpty()) {
            return String.format("https://%s/%s", cloudfrontDomain, s3Key);
        }
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                bucketName, region, s3Key);
    }

    /**
     * Content-Type 결정
     */
    private String determineContentType(File file) {
        String fileName = file.getName().toLowerCase();

        if (fileName.endsWith(".m3u8")) {
            return "application/vnd.apple.mpegurl";
        } else if (fileName.endsWith(".ts")) {
            return "video/mp2t";
        } else if (fileName.endsWith(".mp4")) {
            return "video/mp4";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        }

        return "application/octet-stream";
    }
}
