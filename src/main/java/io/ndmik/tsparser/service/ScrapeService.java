package io.ndmik.tsparser.service;

import io.ndmik.tsparser.dto.JobSyncResult;
import io.ndmik.tsparser.dto.ScrapedJob;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ScrapeService {

    private final TechstarsJobsClient jobsClient;
    private final JobSyncService jobSyncService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public ScrapeService(TechstarsJobsClient jobsClient,
                         JobSyncService jobSyncService) {
        this.jobsClient = jobsClient;
        this.jobSyncService = jobSyncService;
    }

    public JobSyncResult scrapeAndSync() {
        if (!running.compareAndSet(false, true)) {
            throw new ScrapeAlreadyRunningException();
        }

        try {
            List<ScrapedJob> scrapedJobs = jobsClient.fetchJobs();
            return jobSyncService.sync(scrapedJobs);
        } finally {
            running.set(false);
        }
    }
}
