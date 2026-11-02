package com.careerpick.service;

import com.careerpick.entity.Job;
import com.careerpick.repository.JobRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JobServiceTest {
    private final JobRepository jobs = mock(JobRepository.class);
    private final JobService service = new JobService(jobs);

    private void useSampleJobs() {
        List<Job> sampleJobs = List.of(
            new Job("네오테크", "N", "#6556df", "신입 개발자 공개채용", "개발", "신입", "서울", "D-3", List.of("백엔드")),
            new Job("오늘마켓", "O", "#f2a800", "콘텐츠 마케터 인턴", "마케팅", "인턴", "서울", "D-12", List.of("콘텐츠")),
            new Job("데이터포레스트", "D", "#1f8768", "AI Engineer 인턴", "개발", "인턴", "대전", "D-20", List.of("AI"))
        );
        for (int i = 0; i < sampleJobs.size(); i++) {
            org.springframework.test.util.ReflectionTestUtils.setField(sampleJobs.get(i), "id", (long) i + 1);
        }
        when(jobs.findAllByOrderByIdAsc()).thenReturn(sampleJobs);
    }

    @Test void searchesByCompanyAndType() {
        useSampleJobs();
        assertThat(service.find("네오", "전체")).hasSize(1);
        assertThat(service.find("인턴", "전체")).hasSize(2);
    }

    @Test void filtersByCategory() {
        useSampleJobs();
        assertThat(service.find("", "개발")).isNotEmpty().allMatch(job -> job.category().equals("개발"));
    }
}
