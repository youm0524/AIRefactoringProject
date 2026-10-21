package com.careerpick.job;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "http://localhost:5173")
public class JobController {
    private final JobService jobService;
    public JobController(JobService jobService) { this.jobService = jobService; }

    @GetMapping
    public List<Job> jobs(@RequestParam(defaultValue = "") String query,
                          @RequestParam(defaultValue = "전체") String category) {
        return jobService.find(query, category);
    }
}
