package com.careerpick.service;

import com.careerpick.dto.JobResponse;
import com.careerpick.entity.Job;
import com.careerpick.repository.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Locale;

@Service
public class JobService {
    private final JobRepository jobs;

    public JobService(JobRepository jobs) { this.jobs = jobs; }

    @Transactional(readOnly = true)
    public List<JobResponse> find(String query, String category) {
        String keyword = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        // shortcut: MVP 공고 수가 적어 애플리케이션에서 필터링, 공고가 늘면 DB 검색 쿼리로 교체한다.
        return jobs.findAllByOrderByIdAsc().stream().filter(job -> category == null || category.isBlank() || category.equals("전체") || job.category().equals(category))
            .filter(job -> keyword.isBlank() || searchable(job).contains(keyword)).map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Job findById(long id) {
        return jobs.findById(id).orElse(null);
    }

    private String searchable(Job job) {
        return String.join(" ", job.company(), job.title(), job.category(), job.type(), String.join(" ", job.tags())).toLowerCase(Locale.ROOT);
    }

    private JobResponse toResponse(Job job) {
        return new JobResponse(job.id(), job.company(), job.logo(), job.color(), job.title(), job.category(),
            job.type(), job.location(), job.deadline(), job.tags());
    }
}
