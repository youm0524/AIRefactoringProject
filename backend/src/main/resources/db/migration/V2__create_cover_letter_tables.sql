CREATE TABLE cover_letters (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    job_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_cover_letters_user_job UNIQUE (user_id, job_id),
    INDEX idx_cover_letters_user_updated (user_id, updated_at, id),
    CONSTRAINT fk_cover_letters_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_cover_letters_job FOREIGN KEY (job_id) REFERENCES jobs (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE cover_letter_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cover_letter_id BIGINT NOT NULL,
    question_key VARCHAR(50) NOT NULL,
    question_text TEXT NOT NULL,
    display_order INT NOT NULL,
    max_length INT NOT NULL,
    answer TEXT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_cover_letter_items_key UNIQUE (cover_letter_id, question_key),
    CONSTRAINT uk_cover_letter_items_order UNIQUE (cover_letter_id, display_order),
    CONSTRAINT ck_cover_letter_items_order CHECK (display_order > 0),
    CONSTRAINT ck_cover_letter_items_max_length CHECK (max_length > 0),
    CONSTRAINT fk_cover_letter_items_cover_letter FOREIGN KEY (cover_letter_id) REFERENCES cover_letters (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
