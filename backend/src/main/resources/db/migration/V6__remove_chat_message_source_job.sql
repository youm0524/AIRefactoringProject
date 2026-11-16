ALTER TABLE chat_messages
    DROP FOREIGN KEY fk_chat_messages_source_job,
    DROP COLUMN source_job_id;
