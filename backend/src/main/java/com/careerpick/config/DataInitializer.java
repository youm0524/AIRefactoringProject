package com.careerpick.config;

import com.careerpick.entity.ChatMessageEntity;
import com.careerpick.entity.Job;
import com.careerpick.entity.User;
import com.careerpick.repository.ChatMessageRepository;
import com.careerpick.repository.JobRepository;
import com.careerpick.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Component
public class DataInitializer implements ApplicationRunner {
    private static final String DEMO_EMAIL = "demo@careerpick.kr";
    private final UserRepository users;
    private final JobRepository jobs;
    private final ChatMessageRepository messages;
    private final BCryptPasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository users, JobRepository jobs, ChatMessageRepository messages,
                           BCryptPasswordEncoder passwordEncoder) {
        this.users = users;
        this.jobs = jobs;
        this.messages = messages;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!users.existsByEmailIgnoreCase(DEMO_EMAIL)) {
            users.save(new User("김커리", DEMO_EMAIL, passwordEncoder.encode("career1234")));
        }
        if (jobs.count() == 0) jobs.saveAll(sampleJobs());
        if (messages.count() == 0) {
            jobs.findAllByOrderByIdAsc().forEach(job -> messages.saveAll(List.of(
                new ChatMessageEntity(job, null, "익명지원자", job.company() + " 지원하시는 분들 계신가요?"),
                new ChatMessageEntity(job, null, "취준메이트", "네! 전형 정보 같이 나눠요 🙌")
            )));
        }
    }

    private List<Job> sampleJobs() {
        return List.of(
            new Job("네오테크","N","#6556df","2026 상반기 신입 개발자 공개채용","개발","신입","서울","D-3",List.of("백엔드","프론트엔드")),
            new Job("그린모빌리티","G","#16ad68","서비스 기획 및 데이터 분석 인턴","마케팅","인턴","경기","D-5",List.of("서비스기획","데이터")),
            new Job("라이트스튜디오","L","#ff895c","Product Designer 신입 채용","디자인","신입","서울","D-7",List.of("UX/UI","프로덕트")),
            new Job("한빛금융","H","#2672e8","디지털 금융 서비스 개발자 모집","개발","신입","서울","D-9",List.of("핀테크","Java")),
            new Job("오늘마켓","O","#f2a800","브랜드 콘텐츠 마케터 채용연계형 인턴","마케팅","인턴","서울","D-12",List.of("콘텐츠","브랜드")),
            new Job("클라우드나인","C","#2e9ecb","인사 운영 및 조직문화 담당자","경영지원","신입","경기","D-14",List.of("HR","조직문화")),
            new Job("모노웍스","M","#e05e7e","BX 디자이너 경력직 채용","디자인","경력","서울","D-18",List.of("브랜딩","그래픽")),
            new Job("데이터포레스트","D","#1f8768","AI Research Engineer 인턴십","개발","인턴","대전","D-20",List.of("AI","Python")),
            new Job("피크커머스","P","#8c62d8","재무회계 신입사원 수시채용","경영지원","신입","서울","D-21",List.of("재무","회계"))
        );
    }
}
