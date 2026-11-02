package com.careerpick.service;

import com.careerpick.dto.JoinCommunityResponse;
import com.careerpick.entity.CommunityRoom;
import com.careerpick.entity.Job;
import com.careerpick.entity.User;
import com.careerpick.repository.CommunityRoomMemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommunityServiceTest {
    private final AuthService authService = mock(AuthService.class);
    private final JobService jobService = mock(JobService.class);
    private final CommunityRoomMemberRepository members = mock(CommunityRoomMemberRepository.class);
    private final CommunityService service = new CommunityService(authService, jobService, members);
    private final User user = user();

    @Test void joinsTheRoomConnectedToTheJob() {
        CommunityRoom room = room();
        when(authService.requireUserEntity("Bearer token")).thenReturn(user);
        when(jobService.findById(10)).thenReturn(job(room));

        JoinCommunityResponse response = service.join(10, "Bearer token");

        assertThat(response.room().id()).isEqualTo(20);
        assertThat(response.room().companyName()).isEqualTo("네오테크");
        verify(members).join(1, 20);
    }

    @Test void rejectsMissingJobWithoutCreatingMembership() {
        when(authService.requireUserEntity("Bearer token")).thenReturn(user);

        assertThatThrownBy(() -> service.join(999, "Bearer token"))
            .isInstanceOfSatisfying(ResponseStatusException.class,
                exception -> assertThat(exception.getStatusCode().value()).isEqualTo(404));
        verifyNoInteractions(members);
    }

    private User user() {
        User user = new User("김커리", "demo@careerpick.kr", "hash");
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    private CommunityRoom room() {
        CommunityRoom room = new CommunityRoom("네오테크");
        ReflectionTestUtils.setField(room, "id", 20L);
        return room;
    }

    private Job job(CommunityRoom room) {
        return new Job(room, "네오테크", "N", "#6556df", "개발자 채용", "개발", "신입", "서울", "D-3", List.of("백엔드"));
    }
}
