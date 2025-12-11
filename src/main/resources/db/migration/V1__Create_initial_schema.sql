-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL UNIQUE,
    user_id BIGINT,
    display_name VARCHAR(255),
    reputation INTEGER,
    user_type VARCHAR(50),
    CONSTRAINT uk_users_account_id UNIQUE (account_id)
);

CREATE INDEX IF NOT EXISTS idx_user_account_id ON users(account_id);
CREATE INDEX IF NOT EXISTS idx_user_user_id ON users(user_id);

-- 创建标签表
CREATE TABLE IF NOT EXISTS tags (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE INDEX IF NOT EXISTS idx_tag_name ON tags(name);

-- 创建问题表
CREATE TABLE IF NOT EXISTS questions (
    question_id BIGINT PRIMARY KEY,
    title VARCHAR(500),
    body TEXT,
    answered BOOLEAN,
    answer_count INTEGER,
    score INTEGER,
    creation_date TIMESTAMP,
    last_activity_date TIMESTAMP,
    accepted_answer_id BIGINT,
    view_count INTEGER,
    owner_account_id BIGINT,
    CONSTRAINT fk_question_owner FOREIGN KEY (owner_account_id) REFERENCES users(account_id)
);

CREATE INDEX IF NOT EXISTS idx_question_creation_date ON questions(creation_date);
CREATE INDEX IF NOT EXISTS idx_question_score ON questions(score);
CREATE INDEX IF NOT EXISTS idx_question_answered ON questions(answered);

-- 创建问题标签关联表
CREATE TABLE IF NOT EXISTS question_tags (
    question_id BIGINT NOT NULL,
    tag_name VARCHAR(100) NOT NULL,
    PRIMARY KEY (question_id, tag_name),
    CONSTRAINT fk_qt_question FOREIGN KEY (question_id) REFERENCES questions(question_id) ON DELETE CASCADE,
    CONSTRAINT fk_qt_tag FOREIGN KEY (tag_name) REFERENCES tags(name) ON DELETE CASCADE
);

-- 创建回答表
CREATE TABLE IF NOT EXISTS answers (
    answer_id BIGINT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    body TEXT,
    score INTEGER,
    accepted BOOLEAN,
    creation_date TIMESTAMP,
    owner_account_id BIGINT,
    CONSTRAINT fk_answer_question FOREIGN KEY (question_id) REFERENCES questions(question_id) ON DELETE CASCADE,
    CONSTRAINT fk_answer_owner FOREIGN KEY (owner_account_id) REFERENCES users(account_id)
);

CREATE INDEX IF NOT EXISTS idx_answer_question_id ON answers(question_id);
CREATE INDEX IF NOT EXISTS idx_answer_accepted ON answers(accepted);
CREATE INDEX IF NOT EXISTS idx_answer_score ON answers(score);

-- 创建评论表
CREATE TABLE IF NOT EXISTS comments (
    comment_id BIGINT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    post_type VARCHAR(20) NOT NULL,
    body TEXT,
    score INTEGER,
    creation_date TIMESTAMP,
    owner_account_id BIGINT,
    question_id BIGINT,
    answer_id BIGINT,
    CONSTRAINT fk_comment_owner FOREIGN KEY (owner_account_id) REFERENCES users(account_id),
    CONSTRAINT fk_comment_question FOREIGN KEY (question_id) REFERENCES questions(question_id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_answer FOREIGN KEY (answer_id) REFERENCES answers(answer_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_comment_post_id ON comments(post_id);
CREATE INDEX IF NOT EXISTS idx_comment_post_type ON comments(post_type);







