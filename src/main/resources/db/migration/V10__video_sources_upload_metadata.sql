ALTER TABLE Videos
    ADD COLUMN source_type VARCHAR(32) NULL,
    ADD COLUMN original_filename VARCHAR(255) NULL,
    ADD COLUMN stored_filename VARCHAR(255) NULL,
    ADD COLUMN content_type VARCHAR(100) NULL,
    ADD COLUMN file_size_bytes BIGINT NULL,
    MODIFY COLUMN video_url VARCHAR(2048) NULL;

UPDATE Videos
SET source_type = CASE
    WHEN video_url LIKE '/uploads/videos/%' THEN 'UPLOAD'
    WHEN video_url LIKE '%youtube.com/%' OR video_url LIKE '%youtu.be/%' THEN 'YOUTUBE'
    WHEN video_url IS NOT NULL AND video_url <> '' THEN 'DIRECT_URL'
    ELSE source_type
END
WHERE source_type IS NULL;
