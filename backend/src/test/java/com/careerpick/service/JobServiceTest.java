package com.careerpick.service;

import com.careerpick.entity.Company;
import com.careerpick.entity.Job;
import com.careerpick.repository.JobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JobServiceTest {
    private final JobRepository jobs = mock(JobRepository.class);
    private final JobService service = new JobService(jobs);

    private void useSampleJobs() {
        when(jobs.findAllByOrderByIdAsc()).thenReturn(List.of(
            job(1, "네오테크", "신입 개발자 공개채용", "개발", "신입", List.of("백엔드")),
            job(2, "오늘마켓", "콘텐츠 마케터 인턴", "마케팅", "인턴", List.of("콘텐츠")),
            job(3, "데이터포레스트", "AI Engineer 인턴", "개발", "인턴", List.of("AI"))
        ));
    }

    private Job job(long id, String company, String title, String category, String type, List<String> tags) {
        Company companyEntity = new Company(company, company.substring(0, 1), "#000000");
        ReflectionTestUtils.setField(companyEntity, "id", id + 10);
        Job job = new Job(companyEntity, title, category, type,
            "서울", "D-3", tags);
        ReflectionTestUtils.setField(job, "id", id);
        return job;
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

    @Test void returnsCompanyFieldsFromTheCompanyEntity() {
        useSampleJobs();

        var result = service.find("네오", "전체").get(0);

        assertThat(result.companyId()).isEqualTo(11);
        assertThat(result.company()).isEqualTo("네오테크");
        assertThat(result.logo()).isEqualTo("네");
        assertThat(result.color()).isEqualTo("#000000");
    }
}
