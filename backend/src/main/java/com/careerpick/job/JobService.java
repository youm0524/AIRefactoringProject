package com.careerpick.job;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Locale;

@Service
public class JobService {
    private final List<Job> jobs = List.of(
        new Job(1,"네오테크","N","#6556df","2026 상반기 신입 개발자 공개채용","개발","신입","서울","D-3",List.of("백엔드","프론트엔드")),
        new Job(2,"그린모빌리티","G","#16ad68","서비스 기획 및 데이터 분석 인턴","마케팅","인턴","경기","D-5",List.of("서비스기획","데이터")),
        new Job(3,"라이트스튜디오","L","#ff895c","Product Designer 신입 채용","디자인","신입","서울","D-7",List.of("UX/UI","프로덕트")),
        new Job(4,"한빛금융","H","#2672e8","디지털 금융 서비스 개발자 모집","개발","신입","서울","D-9",List.of("핀테크","Java")),
        new Job(5,"오늘마켓","O","#f2a800","브랜드 콘텐츠 마케터 채용연계형 인턴","마케팅","인턴","서울","D-12",List.of("콘텐츠","브랜드")),
        new Job(6,"클라우드나인","C","#2e9ecb","인사 운영 및 조직문화 담당자","경영지원","신입","경기","D-14",List.of("HR","조직문화")),
        new Job(7,"모노웍스","M","#e05e7e","BX 디자이너 경력직 채용","디자인","경력","서울","D-18",List.of("브랜딩","그래픽")),
        new Job(8,"데이터포레스트","D","#1f8768","AI Research Engineer 인턴십","개발","인턴","대전","D-20",List.of("AI","Python")),
        new Job(9,"피크커머스","P","#8c62d8","재무회계 신입사원 수시채용","경영지원","신입","서울","D-21",List.of("재무","회계"))
    );

    public List<Job> find(String query, String category) {
        String keyword = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return jobs.stream().filter(job -> category == null || category.isBlank() || category.equals("전체") || job.category().equals(category))
            .filter(job -> keyword.isBlank() || searchable(job).contains(keyword)).toList();
    }

    public Job findById(long id) {
        return jobs.stream().filter(job -> job.id() == id).findFirst().orElse(null);
    }

    private String searchable(Job job) {
        return String.join(" ", job.company(), job.title(), job.category(), job.type(), String.join(" ", job.tags())).toLowerCase(Locale.ROOT);
    }
}
