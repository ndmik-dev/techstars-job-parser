package io.ndmik.tsparser.controller;

import io.ndmik.tsparser.dto.JobFilter;
import io.ndmik.tsparser.dto.JobResponse;
import io.ndmik.tsparser.dto.PageResponse;
import io.ndmik.tsparser.service.JobQueryService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobQueryService jobQueryService;

    public JobController(JobQueryService jobQueryService) {
        this.jobQueryService = jobQueryService;
    }

    @GetMapping
    public PageResponse<JobResponse> findJobs(@RequestParam(required = false) String q,
                                              @RequestParam(required = false) String location,
                                              @RequestParam(required = false) String company,
                                              @RequestParam(required = false) String tag,
                                              @RequestParam(required = false) String remoteType,
                                              @RequestParam(required = false) String seniority,
                                              @RequestParam(defaultValue = "true") Boolean active,
                                              @PageableDefault(size = 20, sort = "lastSeenAt") Pageable pageable) {
        JobFilter filter = new JobFilter(q, location, company, tag, remoteType, seniority, active);
        return PageResponse.from(jobQueryService.findJobs(filter, pageable));
    }

    @GetMapping("/{id}")
    public JobResponse findJob(@PathVariable Long id) {
        return jobQueryService.findJob(id);
    }
}
