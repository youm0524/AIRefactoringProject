ALTER TABLE chat_messages
    DROP FOREIGN KEY fk_chat_messages_job,
    DROP INDEX idx_chat_messages_job_id,
    CHANGE COLUMN job_id source_job_id BIGINT NULL,
    ADD COLUMN room_id BIGINT NULL AFTER source_job_id,
    ADD COLUMN client_message_id VARCHAR(36) NULL AFTER user_id;

UPDATE chat_messages message
JOIN jobs job ON job.id = message.source_job_id
SET message.room_id = job.community_room_id;

ALTER TABLE chat_messages
    MODIFY room_id BIGINT NOT NULL,
    ADD INDEX idx_chat_messages_room_created (room_id, created_at DESC, id DESC),
    ADD CONSTRAINT uk_chat_messages_user_client UNIQUE (user_id, client_message_id),
    ADD CONSTRAINT fk_chat_messages_source_job FOREIGN KEY (source_job_id) REFERENCES jobs (id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_chat_messages_room FOREIGN KEY (room_id) REFERENCES community_rooms (id) ON DELETE CASCADE;
