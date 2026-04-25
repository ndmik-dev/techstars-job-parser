package io.ndmik.tsparser.service;

import io.ndmik.tsparser.dto.JobFilter;
import io.ndmik.tsparser.dto.JobResponse;
import io.ndmik.tsparser.repository.JobRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobQueryService {

    private final JobRepository jobRepository;

    public JobQueryService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Transactional(readOnly = true)
    public Page<JobResponse> findJobs(JobFilter filter, Pageable pageable) {
        return jobRepository.findAll(JobSpecifications.byFilter(filter), pageable)
                .map(JobResponse::from);
    }

    @Transactional(readOnly = true)
    public JobResponse findJob(Long id) {
        return jobRepository.findWithCompanyAndTagsById(id)
                .map(JobResponse::from)
                .orElseThrow(() -> new JobNotFoundException(id));
    }
}
