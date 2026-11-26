package com.careerpick.service;

import com.careerpick.dto.LoginRequest;
import com.careerpick.dto.LoginResponse;
import com.careerpick.dto.UserView;
import com.careerpick.entity.User;
import com.careerpick.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

@Service
public class AuthService {
    private static final Duration SESSION_TTL = Duration.ofHours(24);
    private static final String SESSION_PREFIX = "auth:session:";
    private final UserRepository users;
    private final StringRedisTemplate redis;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository users, StringRedisTemplate redis, BCryptPasswordEncoder passwordEncoder) {
        this.users = users;
        this.redis = redis;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email()).orElse(null);
        if (user == null || !passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        String token = UUID.randomUUID().toString();
        redis.opsForValue().set(SESSION_PREFIX + token, user.id().toString(), SESSION_TTL);
        return new LoginResponse(token, toView(user));
    }

    public UserView requireUser(String authorization) {
        return toView(requireUserEntity(authorization));
    }

    public User requireUserEntity(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        return requireUserByToken(authorization.substring(7));
    }

    public AuthenticatedSession requireWebSocketSession(String token) {
        if (token == null || token.isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        String key = SESSION_PREFIX + token;
        User user = requireUserByToken(token);
        Long ttlMillis = redis.getExpire(key, TimeUnit.MILLISECONDS);
        if (ttlMillis == null || ttlMillis <= 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 만료되었습니다.");
        }
        return new AuthenticatedSession(user.id(), user.name(), token, Instant.now().plusMillis(ttlMillis));
    }

    private User requireUserByToken(String token) {
        String userId = redis.opsForValue().get(SESSION_PREFIX + token);
        if (userId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 만료되었습니다.");
        try {
            return users.findById(Long.parseLong(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다."));
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 정보가 올바르지 않습니다.");
        }
    }

    private UserView toView(User user) { return new UserView(user.id(), user.name(), user.email()); }

    public record AuthenticatedSession(long userId, String userName, String token, Instant expiresAt) {}
}
