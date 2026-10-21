package com.careerpick.job;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class JobServiceTest {
    private final JobService service = new JobService();

    @Test void searchesByCompanyAndType() {
        assertThat(service.find("네오", "전체")).hasSize(1);
        assertThat(service.find("인턴", "전체")).hasSize(3);
    }

    @Test void filtersByCategory() {
        assertThat(service.find("", "개발")).isNotEmpty().allMatch(job -> job.category().equals("개발"));
    }
}
