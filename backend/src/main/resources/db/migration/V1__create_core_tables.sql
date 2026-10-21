CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(60) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE jobs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company VARCHAR(100) NOT NULL,
    logo VARCHAR(10) NOT NULL,
    color VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    category VARCHAR(30) NOT NULL,
    type VARCHAR(30) NOT NULL,
    location VARCHAR(100) NOT NULL,
    deadline VARCHAR(30) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_jobs_category (category),
    INDEX idx_jobs_company (company)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE job_tags (
    job_id BIGINT NOT NULL,
    tag VARCHAR(50) NOT NULL,
    INDEX idx_job_tags_job_id (job_id),
    CONSTRAINT fk_job_tags_job FOREIGN KEY (job_id) REFERENCES jobs (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE chat_messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    job_id BIGINT NOT NULL,
    user_id BIGINT NULL,
    author VARCHAR(50) NOT NULL,
    text VARCHAR(120) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_chat_messages_job_id (job_id, id),
    INDEX idx_chat_messages_user_id (user_id),
    CONSTRAINT fk_chat_messages_job FOREIGN KEY (job_id) REFERENCES jobs (id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_messages_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
