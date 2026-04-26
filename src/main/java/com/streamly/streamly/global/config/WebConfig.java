package com.streamly.streamly.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web MVC 설정
 * - CORS 설정
 * - 정적 리소스 핸들러 설정 (썸네일, 업로드 파일 등)
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${video.upload.directory:./uploads}")
    private String uploadDir;

    @Value("${video.encoded.directory:./encoded}")
    private String encodedDir;

    /**
     * CORS 설정
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        // 정적 리소스 경로도 CORS 허용 (FE에서 직접 접근)
        registry.addMapping("/encoded/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        registry.addMapping("/thumbnails/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 정적 리소스 핸들러 설정
     * /thumbnails/** 요청을 uploads/thumbnails/ 디렉토리로 매핑
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 썸네일 이미지 경로 설정
        String thumbnailPath = Paths.get(uploadDir, "thumbnails")
                .toAbsolutePath()
                .normalize()
                .toUri()
                .toString();

        registry.addResourceHandler("/thumbnails/**")
                .addResourceLocations(thumbnailPath)
                .setCachePeriod(3600); // 1시간 캐시

        // 업로드 파일 경로 설정 (필요한 경우)
        String uploadPath = Paths.get(uploadDir)
                .toAbsolutePath()
                .normalize()
                .toUri()
                .toString();

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath)
                .setCachePeriod(3600);

        // 인코딩된 HLS 파일 경로 설정
        String encodedPath = Paths.get(encodedDir)
                .toAbsolutePath()
                .normalize()
                .toUri()
                .toString();

        registry.addResourceHandler("/encoded/**")
                .addResourceLocations(encodedPath)
                .setCachePeriod(0); // HLS는 캐시 안함
    }
}
