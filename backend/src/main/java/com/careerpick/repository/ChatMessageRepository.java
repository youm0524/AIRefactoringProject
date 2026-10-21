package com.careerpick.repository;

import com.careerpick.entity.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {
    List<ChatMessageEntity> findByJobIdOrderByIdAsc(long jobId);
}
