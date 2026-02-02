-- videos 테이블이 이미 있다면 삭제
DROP TABLE IF EXISTS videos;

-- videos 테이블 생성
CREATE TABLE videos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- 기본 정보
    title VARCHAR(200) NOT NULL,
    description TEXT,
    uploader_id BIGINT NOT NULL,
    
    -- 원본 파일 정보
    original_filename VARCHAR(255) NOT NULL,
    original_file_size BIGINT,
    original_file_path VARCHAR(500),
    
    -- S3 정보
    s3_key VARCHAR(500),
    s3_url VARCHAR(500),
    cloudfront_url VARCHAR(500),
    
    -- 영상 메타데이터
    duration_seconds INT,
    resolution VARCHAR(20),
    video_codec VARCHAR(20),
    audio_codec VARCHAR(20),
    thumbnail_url VARCHAR(500),
    
    -- 처리 상태
    status VARCHAR(20) NOT NULL DEFAULT 'UPLOADING',
    encoding_progress INT DEFAULT 0,
    
    -- 조회수 및 카테고리
    view_count BIGINT DEFAULT 0,
    category VARCHAR(50),
    age_rating VARCHAR(20),
    
    -- 승인 상태
    approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    rejection_reason VARCHAR(500),
    
    -- 타임스탬프
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    published_at TIMESTAMP,
    
    -- 외래 키
    CONSTRAINT fk_videos_uploader FOREIGN KEY (uploader_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 인덱스 생성
CREATE INDEX idx_videos_status ON videos(status);
CREATE INDEX idx_videos_approval ON videos(approval_status);
CREATE INDEX idx_videos_uploader ON videos(uploader_id);
CREATE INDEX idx_videos_category ON videos(category);
CREATE INDEX idx_videos_published ON videos(published_at);
CREATE INDEX idx_videos_view_count ON videos(view_count);
