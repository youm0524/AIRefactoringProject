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
SELECT message.user_id, job.community_room_id, MIN(message.created_at)
FROM chat_messages message
JOIN jobs job ON job.id = message.job_id
WHERE message.user_id IS NOT NULL
GROUP BY message.user_id, job.community_room_id;
