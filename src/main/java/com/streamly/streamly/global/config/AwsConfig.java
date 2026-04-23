package com.streamly.streamly.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * AWS SDK 설정
 * - 로컬: aws.endpoint 값이 있으면 MinIO로 연결
 * - 운영: aws.endpoint 미설정 시 실제 AWS S3로 연결
 */
@Configuration
public class AwsConfig {

    @Value("${aws.access-key}")
    private String accessKey;

    @Value("${aws.secret-key}")
    private String secretKey;

    @Value("${aws.region}")
    private String region;

    @Value("${aws.endpoint:}")
    private String endpoint;  // 로컬(MinIO)일 때만 설정, 운영은 비워둠

    @Bean
    public AwsBasicCredentials awsCredentials() {
        return AwsBasicCredentials.create(accessKey, secretKey);
    }

    @Bean
    public S3Client s3Client() {
        var builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials()));

        // MinIO 사용 시 (로컬 환경)
        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint))
                   .serviceConfiguration(S3Configuration.builder()
                           .pathStyleAccessEnabled(true)  // MinIO는 path-style 필요
                           .build());
        }

        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        var builder = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials()));

        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        return builder.build();
    }
}
