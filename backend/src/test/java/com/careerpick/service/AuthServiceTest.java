package com.careerpick.service;

import com.careerpick.dto.LoginRequest;
import com.careerpick.dto.LoginResponse;
import com.careerpick.entity.User;
import com.careerpick.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final User user = testUser();
    private final AuthService service = new AuthService(users, redis, encoder);

    @Test void logsInAndResolvesSession() {
        when(users.findByEmailIgnoreCase("demo@careerpick.kr")).thenReturn(Optional.of(user));
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenReturn("1");
        when(users.findById(1L)).thenReturn(Optional.of(user));

        LoginResponse login = service.login(new LoginRequest("demo@careerpick.kr", "career1234"));

        assertThat(service.requireUser("Bearer " + login.token()).email()).isEqualTo("demo@careerpick.kr");
        verify(values).set(startsWith("auth:session:"), eq("1"), any(Duration.class));
    }

    @Test void rejectsWrongPassword() {
        when(users.findByEmailIgnoreCase("demo@careerpick.kr")).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> service.login(new LoginRequest("demo@careerpick.kr", "wrongpass"))).isInstanceOf(ResponseStatusException.class);
    }

    private User testUser() {
        User user = new User("김커리", "demo@careerpick.kr", encoder.encode("career1234"));
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }
}
