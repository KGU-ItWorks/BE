package com.streamly.streamly.global.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class FFmpegService {

    @Value("${ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    @Value("${ffprobe.path:ffprobe}")
    private String ffprobePath;

    /**
     * 영상을 HLS 포맷으로 인코딩
     * 
     * @param inputPath 원본 영상 파일 경로
     * @param outputDir 출력 디렉토리 (HLS 파일들이 저장될 위치)
     * @return 생성된 마스터 플레이리스트 파일 경로
     */
    public String encodeToHLS(String inputPath, String outputDir) throws IOException, InterruptedException {
        log.info("Starting HLS encoding for: {}", inputPath);

        // 출력 디렉토리 생성
        Path outputPath = Paths.get(outputDir);
        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath);
        }

        // 다중 해상도 HLS 생성
        String masterPlaylist = outputDir + "/master.m3u8";
        
        // 360p 인코딩
        encode360p(inputPath, outputDir);
        
        // 480p 인코딩
        encode480p(inputPath, outputDir);
        
        // 720p 인코딩
        encode720p(inputPath, outputDir);

        // 마스터 플레이리스트 생성
        createMasterPlaylist(outputDir);

        log.info("HLS encoding completed. Master playlist: {}", masterPlaylist);
        return masterPlaylist;
    }

    /**
     * 360p 인코딩
     */
    private void encode360p(String inputPath, String outputDir) throws IOException, InterruptedException {
        String output360 = outputDir + "/360p.m3u8";
        
        CommandLine cmdLine = new CommandLine(ffmpegPath);
        cmdLine.addArgument("-i");
        cmdLine.addArgument(inputPath);
        cmdLine.addArgument("-vf");
        cmdLine.addArgument("scale=-2:360");
        cmdLine.addArgument("-c:v");
        cmdLine.addArgument("libx264");
        cmdLine.addArgument("-preset");
        cmdLine.addArgument("medium");
        cmdLine.addArgument("-b:v");
        cmdLine.addArgument("800k");
        cmdLine.addArgument("-c:a");
        cmdLine.addArgument("aac");
        cmdLine.addArgument("-b:a");
        cmdLine.addArgument("128k");
        cmdLine.addArgument("-hls_time");
        cmdLine.addArgument("4");
        cmdLine.addArgument("-hls_playlist_type");
        cmdLine.addArgument("vod");
        cmdLine.addArgument("-hls_segment_filename");
        cmdLine.addArgument(outputDir + "/360p_%03d.ts");
        cmdLine.addArgument(output360);

        executeCommand(cmdLine);
    }

    /**
     * 480p 인코딩
     */
    private void encode480p(String inputPath, String outputDir) throws IOException, InterruptedException {
        String output480 = outputDir + "/480p.m3u8";
        
        CommandLine cmdLine = new CommandLine(ffmpegPath);
        cmdLine.addArgument("-i");
        cmdLine.addArgument(inputPath);
        cmdLine.addArgument("-vf");
        cmdLine.addArgument("scale=-2:480");
        cmdLine.addArgument("-c:v");
        cmdLine.addArgument("libx264");
        cmdLine.addArgument("-preset");
        cmdLine.addArgument("medium");
        cmdLine.addArgument("-b:v");
        cmdLine.addArgument("1400k");
        cmdLine.addArgument("-c:a");
        cmdLine.addArgument("aac");
        cmdLine.addArgument("-b:a");
        cmdLine.addArgument("128k");
        cmdLine.addArgument("-hls_time");
        cmdLine.addArgument("4");
        cmdLine.addArgument("-hls_playlist_type");
        cmdLine.addArgument("vod");
        cmdLine.addArgument("-hls_segment_filename");
        cmdLine.addArgument(outputDir + "/480p_%03d.ts");
        cmdLine.addArgument(output480);

        executeCommand(cmdLine);
    }

    /**
     * 720p 인코딩
     */
    private void encode720p(String inputPath, String outputDir) throws IOException, InterruptedException {
        String output720 = outputDir + "/720p.m3u8";
        
        CommandLine cmdLine = new CommandLine(ffmpegPath);
        cmdLine.addArgument("-i");
        cmdLine.addArgument(inputPath);
        cmdLine.addArgument("-vf");
        cmdLine.addArgument("scale=-2:720");
        cmdLine.addArgument("-c:v");
        cmdLine.addArgument("libx264");
        cmdLine.addArgument("-preset");
        cmdLine.addArgument("medium");
        cmdLine.addArgument("-b:v");
        cmdLine.addArgument("2800k");
        cmdLine.addArgument("-c:a");
        cmdLine.addArgument("aac");
        cmdLine.addArgument("-b:a");
        cmdLine.addArgument("192k");
        cmdLine.addArgument("-hls_time");
        cmdLine.addArgument("4");
        cmdLine.addArgument("-hls_playlist_type");
        cmdLine.addArgument("vod");
        cmdLine.addArgument("-hls_segment_filename");
        cmdLine.addArgument(outputDir + "/720p_%03d.ts");
        cmdLine.addArgument(output720);

        executeCommand(cmdLine);
    }

    /**
     * 마스터 플레이리스트 생성
     */
    private void createMasterPlaylist(String outputDir) throws IOException {
        String masterContent = """
                #EXTM3U
                #EXT-X-VERSION:3
                #EXT-X-STREAM-INF:BANDWIDTH=928000,RESOLUTION=640x360
                360p.m3u8
                #EXT-X-STREAM-INF:BANDWIDTH=1528000,RESOLUTION=854x480
                480p.m3u8
                #EXT-X-STREAM-INF:BANDWIDTH=2992000,RESOLUTION=1280x720
                720p.m3u8
                """;

        Path masterPath = Paths.get(outputDir, "master.m3u8");
        Files.writeString(masterPath, masterContent);
    }

    /**
     * 영상 메타데이터 추출 (ffprobe 사용)
     */
    public VideoMetadata extractMetadata(String videoPath) throws IOException, InterruptedException {
        log.info("Extracting metadata from: {}", videoPath);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        CommandLine cmdLine = new CommandLine(ffprobePath);
        cmdLine.addArgument("-v");
        cmdLine.addArgument("error");
        cmdLine.addArgument("-show_entries");
        cmdLine.addArgument("stream=width,height,codec_name,duration");
        cmdLine.addArgument("-show_entries");
        cmdLine.addArgument("format=duration");
        cmdLine.addArgument("-of");
        cmdLine.addArgument("default=noprint_wrappers=1");
        cmdLine.addArgument(videoPath);

        DefaultExecutor executor = new DefaultExecutor();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
        executor.setStreamHandler(streamHandler);
        executor.execute(cmdLine);

        String output = outputStream.toString();
        log.debug("FFprobe output: {}", output);

        return parseMetadata(output);
    }

    /**
     * FFprobe 출력 파싱
     */
    private VideoMetadata parseMetadata(String output) {
        VideoMetadata metadata = new VideoMetadata();

        // Duration 추출
        Pattern durationPattern = Pattern.compile("duration=(\\d+\\.?\\d*)");
        Matcher durationMatcher = durationPattern.matcher(output);
        if (durationMatcher.find()) {
            double duration = Double.parseDouble(durationMatcher.group(1));
            metadata.setDurationSeconds((int) duration);
        }

        // Width, Height 추출
        Pattern widthPattern = Pattern.compile("width=(\\d+)");
        Pattern heightPattern = Pattern.compile("height=(\\d+)");
        Matcher widthMatcher = widthPattern.matcher(output);
        Matcher heightMatcher = heightPattern.matcher(output);
        
        if (widthMatcher.find() && heightMatcher.find()) {
            int width = Integer.parseInt(widthMatcher.group(1));
            int height = Integer.parseInt(heightMatcher.group(1));
            metadata.setResolution(width + "x" + height);
        }

        // Codec 추출
        Pattern codecPattern = Pattern.compile("codec_name=(\\w+)");
        Matcher codecMatcher = codecPattern.matcher(output);
        if (codecMatcher.find()) {
            metadata.setVideoCodec(codecMatcher.group(1));
        }
        if (codecMatcher.find()) {
            metadata.setAudioCodec(codecMatcher.group(1));
        }

        return metadata;
    }

    /**
     * FFmpeg 명령 실행
     */
    private void executeCommand(CommandLine cmdLine) throws IOException, InterruptedException {
        log.info("Executing FFmpeg command: {}", cmdLine);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();

        DefaultExecutor executor = new DefaultExecutor();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream, errorStream);
        executor.setStreamHandler(streamHandler);
        
        try {
            executor.execute(cmdLine);
            log.info("FFmpeg command executed successfully");
        } catch (IOException e) {
            log.error("FFmpeg execution failed: {}", errorStream.toString());
            throw e;
        }
    }

    /**
     * 영상 메타데이터 DTO
     */
    @lombok.Data
    public static class VideoMetadata {
        private Integer durationSeconds;
        private String resolution;
        private String videoCodec;
        private String audioCodec;
    }
}
