-- 删除旧的 comments 表
DROP TABLE IF EXISTS comments CASCADE;

-- 创建问题评论表
CREATE TABLE IF NOT EXISTS question_comments (
    comment_id BIGINT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    body TEXT,
    score INTEGER,
    creation_date TIMESTAMP,
    owner_account_id BIGINT,
    content_license VARCHAR(100),
    CONSTRAINT fk_question_comment_owner FOREIGN KEY (owner_account_id) REFERENCES users(account_id),
    CONSTRAINT fk_question_comment_question FOREIGN KEY (question_id) REFERENCES questions(question_id) ON DELETE CASCADE
);

-- 创建回答评论表
CREATE TABLE IF NOT EXISTS answer_comments (
    comment_id BIGINT PRIMARY KEY,
    answer_id BIGINT NOT NULL,
    body TEXT,
    score INTEGER,
    creation_date TIMESTAMP,
    owner_account_id BIGINT,
    content_license VARCHAR(100),
    CONSTRAINT fk_answer_comment_owner FOREIGN KEY (owner_account_id) REFERENCES users(account_id),
    CONSTRAINT fk_answer_comment_answer FOREIGN KEY (answer_id) REFERENCES answers(answer_id) ON DELETE CASCADE
);

-- 为问题评论表创建索引
CREATE INDEX IF NOT EXISTS idx_question_comment_question_id ON question_comments(question_id);
CREATE INDEX IF NOT EXISTS idx_question_comment_creation_date ON question_comments(creation_date);

-- 为回答评论表创建索引
CREATE INDEX IF NOT EXISTS idx_answer_comment_answer_id ON answer_comments(answer_id);
CREATE INDEX IF NOT EXISTS idx_answer_comment_creation_date ON answer_comments(creation_date);

