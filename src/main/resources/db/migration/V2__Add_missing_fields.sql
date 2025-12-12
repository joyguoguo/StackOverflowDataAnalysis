-- 为用户补充 profile_image、link
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS profile_image VARCHAR(500),
    ADD COLUMN IF NOT EXISTS link VARCHAR(500);

-- 为问题补充 link、closed_date、closed_reason、content_license
ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS link VARCHAR(500),
    ADD COLUMN IF NOT EXISTS closed_date TIMESTAMP,
    ADD COLUMN IF NOT EXISTS closed_reason TEXT,
    ADD COLUMN IF NOT EXISTS content_license VARCHAR(100);

-- 为评论补充 content_license
ALTER TABLE comments
    ADD COLUMN IF NOT EXISTS content_license VARCHAR(100);

-- 为回答补充 last_activity_date 和 content_license
ALTER TABLE answers
    ADD COLUMN IF NOT EXISTS last_activity_date TIMESTAMP,
    ADD COLUMN IF NOT EXISTS content_license VARCHAR(100);



