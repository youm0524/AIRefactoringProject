package com.careerpick.chat;

import com.careerpick.auth.AuthService;
import com.careerpick.auth.UserView;
import com.careerpick.job.Job;
import com.careerpick.job.JobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/jobs/{jobId}/messages")
@CrossOrigin(origins = "http://localhost:5173")
public class ChatController {
    // shortcut: 공고별 대화는 메모리에 저장, 운영 전 데이터베이스와 실시간 전송으로 교체한다.
    private final AtomicLong ids = new AtomicLong(100);
    private final Map<Long, List<ChatMessage>> rooms = new ConcurrentHashMap<>();
    private final JobService jobService;
    private final AuthService authService;

    public ChatController(JobService jobService, AuthService authService) { this.jobService = jobService; this.authService = authService; }

    @GetMapping public List<ChatMessage> messages(@PathVariable long jobId) { return List.copyOf(room(jobId)); }

    @PostMapping
    public List<ChatMessage> send(@PathVariable long jobId, @RequestHeader(name="Authorization", required=false) String authorization,
                                  @Valid @RequestBody SendMessageRequest request) {
        UserView user = authService.requireUser(authorization);
        room(jobId).add(new ChatMessage(ids.incrementAndGet(), user.name(), request.text().trim()));
        return List.copyOf(room(jobId));
    }

    private List<ChatMessage> room(long jobId) {
        Job job = jobService.findById(jobId);
        if (job == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "공고를 찾을 수 없습니다.");
        return rooms.computeIfAbsent(jobId, id -> new CopyOnWriteArrayList<>(List.of(
            new ChatMessage(ids.incrementAndGet(), "익명지원자", job.company() + " 지원하시는 분들 계신가요?"),
            new ChatMessage(ids.incrementAndGet(), "취준메이트", "네! 전형 정보 같이 나눠요 🙌")
        )));
    }
}
