package com.careerpick.service;

import com.careerpick.entity.Job;
import com.careerpick.entity.User;
import com.careerpick.repository.ChatMessageRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class ChatServiceTest {
    private final JobService jobService = mock(JobService.class);
    private final AuthService authService = mock(AuthService.class);
    private final ChatMessageRepository messages = mock(ChatMessageRepository.class);
    private final ChatService service = new ChatService(jobService, authService, messages);

    @Test void savesMessageInTheSelectedJobRoom() {
        Job job = new Job("네오테크", "N", "#6556df", "개발자 채용", "개발", "신입", "서울", "D-3", List.of("백엔드"));
        User user = new User("김커리", "demo@careerpick.kr", "hash");
        when(jobService.findById(1L)).thenReturn(job);
        when(authService.requireUserEntity("Bearer token")).thenReturn(user);
        when(messages.findByJobIdOrderByIdAsc(1L)).thenReturn(List.of());

        assertThat(service.send(1L, "Bearer token", "  반갑습니다  ")).isEmpty();
        verify(messages).save(argThat(message -> message.author().equals("김커리") && message.text().equals("반갑습니다")));
    }
}
