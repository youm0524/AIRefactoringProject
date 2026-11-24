CREATE TABLE community_rooms (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_name VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_community_rooms_company_name UNIQUE (company_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO community_rooms (company_name, created_at)
SELECT DISTINCT company COLLATE utf8mb4_bin, UTC_TIMESTAMP(6)
FROM jobs;

ALTER TABLE jobs ADD COLUMN community_room_id BIGINT NULL;

UPDATE jobs job
JOIN community_rooms room ON job.company COLLATE utf8mb4_bin = room.company_name
SET job.community_room_id = room.id;

ALTER TABLE jobs
    MODIFY community_room_id BIGINT NOT NULL,
    ADD INDEX idx_jobs_community_room_id (community_room_id),
    ADD CONSTRAINT fk_jobs_community_room FOREIGN KEY (community_room_id) REFERENCES community_rooms (id);
