-- 补充 answers 表缺失的列（content_license、last_activity_date）
ALTER TABLE answers
    ADD COLUMN IF NOT EXISTS content_license VARCHAR(100),
    ADD COLUMN IF NOT EXISTS last_activity_date TIMESTAMP;

