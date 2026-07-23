-- video_compositions: 객체 지정 방식을 텍스트 프롬프트/포인트 → 단일 바운딩 박스로 변경
--
-- 배경: JPA ddl-auto=update 는 "이미 행이 존재하는 테이블"에 default 없는 NOT NULL 컬럼을
-- 추가하지 못한다. 그래서 bounding_box(json, NOT NULL) 컬럼이 생성되지 않아 INSERT 가
-- "column bounding_box does not exist" 로 실패한다. 또한 엔티티에서 제거된 object_prompt
-- (NOT NULL) 컬럼이 테이블에 남아 있으면, bounding_box 를 추가해도 INSERT 가 이번엔
-- object_prompt NOT NULL 제약으로 실패한다. 아래를 한 번 실행해 스키마를 정렬한다.
--
-- 주의: 기존 행에는 새 스키마({x,y,width,height}) 값이 없으므로 '{}' 로 채운다.

-- PostgreSQL
ALTER TABLE video_compositions ADD COLUMN IF NOT EXISTS bounding_box json;
UPDATE video_compositions SET bounding_box = '{}'::json WHERE bounding_box IS NULL;
ALTER TABLE video_compositions ALTER COLUMN bounding_box SET NOT NULL;

-- 엔티티에서 제거된 구 컬럼 정리 (존재할 때만)
ALTER TABLE video_compositions DROP COLUMN IF EXISTS object_prompt;
-- 과거 points 실험으로 click_points 가 남아 있다면 함께 제거
ALTER TABLE video_compositions DROP COLUMN IF EXISTS click_points;
