package com.careerpick.auth;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

class AuthServiceTest {
    private final AuthService service = new AuthService();

    @Test void logsInAndResolvesSession() {
        LoginResponse login = service.login(new LoginRequest("demo@careerpick.kr", "career1234"));
        assertThat(service.requireUser("Bearer " + login.token()).email()).isEqualTo("demo@careerpick.kr");
    }

    @Test void rejectsWrongPassword() {
        assertThatThrownBy(() -> service.login(new LoginRequest("demo@careerpick.kr", "wrongpass"))).isInstanceOf(ResponseStatusException.class);
    }
}
