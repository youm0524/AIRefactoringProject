package com.careerpick.service;

import com.careerpick.dto.ChatMessage;
import com.careerpick.entity.ChatMessageEntity;
import com.careerpick.entity.Job;
import com.careerpick.entity.User;
import com.careerpick.repository.ChatMessageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class ChatService {
    private final JobService jobService;
    private final AuthService authService;
    private final ChatMessageRepository messages;

    public ChatService(JobService jobService, AuthService authService, ChatMessageRepository messages) {
        this.jobService = jobService;
        this.authService = authService;
        this.messages = messages;
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> messages(long jobId) {
        requireJob(jobId);
        return findMessages(jobId);
    }

    @Transactional
    public List<ChatMessage> send(long jobId, String authorization, String text) {
        Job job = requireJob(jobId);
        User user = authService.requireUserEntity(authorization);
        messages.save(new ChatMessageEntity(job, user, user.name(), text.trim()));
        return findMessages(jobId);
    }

    private Job requireJob(long jobId) {
        Job job = jobService.findById(jobId);
        if (job == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "공고를 찾을 수 없습니다.");
        return job;
    }

    private List<ChatMessage> findMessages(long jobId) {
        return messages.findByJobIdOrderByIdAsc(jobId).stream()
            .map(message -> new ChatMessage(message.id(), message.author(), message.text())).toList();
    }
}
