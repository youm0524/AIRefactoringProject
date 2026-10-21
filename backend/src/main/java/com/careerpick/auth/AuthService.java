package com.careerpick.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {
    // shortcut: MVP는 인메모리 세션을 사용, 다중 서버 운영 전 영속 세션 또는 JWT로 교체한다.
    private static final String DEMO_EMAIL = "demo@careerpick.kr";
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final String passwordHash = passwordEncoder.encode("career1234");
    private final Map<String, UserView> sessions = new ConcurrentHashMap<>();

    public LoginResponse login(LoginRequest request) {
        if (!DEMO_EMAIL.equalsIgnoreCase(request.email()) || !passwordEncoder.matches(request.password(), passwordHash)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        String token = UUID.randomUUID().toString();
        UserView user = new UserView("김커리", DEMO_EMAIL);
        sessions.put(token, user);
        return new LoginResponse(token, user);
    }

    public UserView requireUser(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        UserView user = sessions.get(authorization.substring(7));
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 만료되었습니다.");
        return user;
    }
}
