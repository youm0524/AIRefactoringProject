CREATE TABLE companies (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    logo VARCHAR(10) NOT NULL,
    color VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_companies_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO companies (name, logo, color)
SELECT DISTINCT company COLLATE utf8mb4_bin, logo, color
FROM jobs;

CREATE TABLE community_rooms (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_community_rooms_company_id UNIQUE (company_id),
    CONSTRAINT fk_community_rooms_company FOREIGN KEY (company_id) REFERENCES companies (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO community_rooms (company_id, created_at)
SELECT id, UTC_TIMESTAMP(6)
FROM companies;

ALTER TABLE jobs ADD COLUMN company_id BIGINT NULL AFTER id;

UPDATE jobs job
JOIN companies company ON company.name = job.company COLLATE utf8mb4_bin
SET job.company_id = company.id;

ALTER TABLE jobs
    MODIFY company_id BIGINT NOT NULL,
    ADD INDEX idx_jobs_company_id (company_id),
    ADD CONSTRAINT fk_jobs_company FOREIGN KEY (company_id) REFERENCES companies (id);

CREATE TABLE community_room_members (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    joined_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_community_room_members_user_room UNIQUE (user_id, room_id),
    INDEX idx_community_room_members_room_id (room_id),
    CONSTRAINT fk_community_room_members_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_community_room_members_room FOREIGN KEY (room_id) REFERENCES community_rooms (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO community_room_members (user_id, room_id, joined_at)
SELECT message.user_id, room.id, MIN(message.created_at)
FROM chat_messages message
JOIN jobs job ON job.id = message.job_id
JOIN community_rooms room ON room.company_id = job.company_id
WHERE message.user_id IS NOT NULL
GROUP BY message.user_id, room.id;

ALTER TABLE chat_messages
    DROP FOREIGN KEY fk_chat_messages_job,
    DROP INDEX idx_chat_messages_job_id,
    ADD COLUMN room_id BIGINT NULL AFTER job_id,
    ADD COLUMN client_message_id VARCHAR(36) NULL AFTER user_id;

UPDATE chat_messages message
JOIN jobs job ON job.id = message.job_id
JOIN community_rooms room ON room.company_id = job.company_id
SET message.room_id = room.id;

ALTER TABLE chat_messages
    MODIFY room_id BIGINT NOT NULL,
    ADD INDEX idx_chat_messages_room_created (room_id, created_at DESC, id DESC),
    ADD CONSTRAINT uk_chat_messages_user_client UNIQUE (user_id, client_message_id),
    ADD CONSTRAINT fk_chat_messages_room FOREIGN KEY (room_id) REFERENCES community_rooms (id) ON DELETE CASCADE,
    DROP COLUMN job_id;

ALTER TABLE jobs
    DROP INDEX idx_jobs_company,
    DROP COLUMN company,
    DROP COLUMN logo,
    DROP COLUMN color;
