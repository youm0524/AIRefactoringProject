CREATE TEMPORARY TABLE company_migration_companies (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    logo VARCHAR(10) NOT NULL,
    color VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_companies_name UNIQUE (name)
) ENGINE=InnoDB;

INSERT INTO company_migration_companies (name, logo, color)
SELECT DISTINCT company COLLATE utf8mb4_bin, logo, color
FROM jobs;

CREATE TEMPORARY TABLE company_migration_rooms (
    room_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    PRIMARY KEY (room_id),
    CONSTRAINT uk_company_migration_rooms_company UNIQUE (company_id)
) ENGINE=InnoDB;

INSERT INTO company_migration_rooms (room_id, company_id)
SELECT DISTINCT job.community_room_id, company.id
FROM jobs job
JOIN company_migration_companies company ON company.name = job.company COLLATE utf8mb4_bin;

CREATE TEMPORARY TABLE company_migration_guard (
    invalid_count BIGINT NOT NULL CHECK (invalid_count = 0)
) ENGINE=InnoDB;

INSERT INTO company_migration_guard (invalid_count)
SELECT
    (SELECT COUNT(*)
     FROM community_rooms room
     LEFT JOIN company_migration_rooms migration ON migration.room_id = room.id
     WHERE migration.room_id IS NULL)
    +
    (SELECT COUNT(*)
     FROM jobs job
     JOIN community_rooms room ON room.id = job.community_room_id
     WHERE job.company COLLATE utf8mb4_bin <> room.company_name);

CREATE TABLE companies (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    logo VARCHAR(10) NOT NULL,
    color VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_companies_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO companies (name, logo, color)
SELECT name, logo, color
FROM company_migration_companies
ORDER BY id;

ALTER TABLE community_rooms ADD COLUMN company_id BIGINT NULL AFTER id;
ALTER TABLE jobs ADD COLUMN company_id BIGINT NULL AFTER id;

UPDATE community_rooms room
JOIN companies company ON company.name = room.company_name COLLATE utf8mb4_bin
SET room.company_id = company.id;

UPDATE jobs job
JOIN companies company ON company.name = job.company COLLATE utf8mb4_bin
SET job.company_id = company.id;

ALTER TABLE community_rooms
    MODIFY company_id BIGINT NOT NULL,
    ADD CONSTRAINT uk_community_rooms_company_id UNIQUE (company_id),
    ADD CONSTRAINT fk_community_rooms_company FOREIGN KEY (company_id) REFERENCES companies (id);

ALTER TABLE jobs
    MODIFY company_id BIGINT NOT NULL,
    ADD INDEX idx_jobs_company_id (company_id),
    ADD CONSTRAINT fk_jobs_company FOREIGN KEY (company_id) REFERENCES companies (id),
    DROP FOREIGN KEY fk_jobs_community_room,
    DROP INDEX idx_jobs_company,
    DROP INDEX idx_jobs_community_room_id,
    DROP COLUMN community_room_id,
    DROP COLUMN company,
    DROP COLUMN logo,
    DROP COLUMN color;

ALTER TABLE community_rooms
    DROP INDEX uk_community_rooms_company_name,
    DROP COLUMN company_name;

DROP TEMPORARY TABLE company_migration_guard;
DROP TEMPORARY TABLE company_migration_rooms;
DROP TEMPORARY TABLE company_migration_companies;
