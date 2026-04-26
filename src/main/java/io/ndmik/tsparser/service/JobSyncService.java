package io.ndmik.tsparser.service;

import io.ndmik.tsparser.dto.JobSyncResult;
import io.ndmik.tsparser.dto.ScrapedJob;
import io.ndmik.tsparser.model.Company;
import io.ndmik.tsparser.model.Job;
import io.ndmik.tsparser.model.ScrapeRun;
import io.ndmik.tsparser.model.Tag;
import io.ndmik.tsparser.repository.CompanyRepository;
import io.ndmik.tsparser.repository.JobRepository;
import io.ndmik.tsparser.repository.ScrapeRunRepository;
import io.ndmik.tsparser.repository.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class JobSyncService {

    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;
    private final TagRepository tagRepository;
    private final ScrapeRunRepository scrapeRunRepository;
    private final TransactionTemplate transactionTemplate;

    public JobSyncService(JobRepository jobRepository,
                          CompanyRepository companyRepository,
                          TagRepository tagRepository,
                          ScrapeRunRepository scrapeRunRepository,
                          TransactionTemplate transactionTemplate) {
        this.jobRepository = jobRepository;
        this.companyRepository = companyRepository;
        this.tagRepository = tagRepository;
        this.scrapeRunRepository = scrapeRunRepository;
        this.transactionTemplate = transactionTemplate;
    }

    public JobSyncResult sync(Collection<ScrapedJob> scrapedJobs) {
        ScrapeRun scrapeRun = scrapeRunRepository.save(ScrapeRun.started());
        List<ScrapedJob> validJobs = validUniqueJobs(scrapedJobs);

        try {
            return transactionTemplate.execute(_ -> syncJobs(scrapeRun, validJobs));
        } catch (RuntimeException exception) {
            markFailed(scrapeRun, exception);
            throw exception;
        }
    }

    private JobSyncResult syncJobs(ScrapeRun scrapeRun, List<ScrapedJob> scrapedJobs) {
        SyncCounters counters = new SyncCounters();
        Set<String> seenExternalIds = new HashSet<>();

        for (ScrapedJob scrapedJob : scrapedJobs) {
            seenExternalIds.add(scrapedJob.externalId());
            syncJob(scrapedJob, counters);
        }

        int deactivatedCount = deactivateMissingJobs(seenExternalIds);
        scrapeRun.complete(seenExternalIds.size(), counters.created, counters.updated, deactivatedCount);
        scrapeRunRepository.save(scrapeRun);

        return new JobSyncResult(
                scrapeRun.getId(),
                seenExternalIds.size(),
                counters.created,
                counters.updated,
                deactivatedCount
        );
    }

    private void syncJob(ScrapedJob scrapedJob, SyncCounters counters) {
        Company company = resolveCompany(scrapedJob);
        Set<Tag> tags = resolveTags(scrapedJob.tags());
        Job job = findOrCreateJob(scrapedJob, company, counters);

        applyScrapedData(job, scrapedJob, company, tags);
        jobRepository.save(job);
    }

    private Job findOrCreateJob(ScrapedJob scrapedJob, Company company, SyncCounters counters) {
        Job job = jobRepository.findByExternalId(scrapedJob.externalId()).orElse(null);
        if (job == null) {
            counters.created++;
            return new Job(
                    scrapedJob.externalId(),
                    scrapedJob.title(),
                    scrapedJob.sourceUrl(),
                    company
            );
        }

        counters.updated++;
        return job;
    }

    private Company resolveCompany(ScrapedJob scrapedJob) {
        return companyRepository.findByNameIgnoreCase(scrapedJob.companyName())
                .map(company -> {
                    company.updateSourceUrl(scrapedJob.companyUrl());
                    return company;
                })
                .orElseGet(() -> companyRepository.save(new Company(scrapedJob.companyName(), scrapedJob.companyUrl())));
    }

    private Set<Tag> resolveTags(List<String> tagNames) {
        Set<Tag> tags = new HashSet<>();
        for (String tagName : tagNames) {
            tags.add(tagRepository.findByNameIgnoreCase(tagName)
                    .orElseGet(() -> tagRepository.save(new Tag(tagName))));
        }
        return tags;
    }

    private void applyScrapedData(Job job, ScrapedJob scrapedJob, Company company, Set<Tag> tags) {
        job.updateDetails(
                scrapedJob.title(),
                scrapedJob.location(),
                scrapedJob.description(),
                scrapedJob.sourceUrl(),
                scrapedJob.remoteType(),
                scrapedJob.seniority(),
                scrapedJob.salaryText(),
                scrapedJob.postedAtText(),
                company
        );
        job.replaceTags(tags);
    }

    private int deactivateMissingJobs(Set<String> seenExternalIds) {
        if (seenExternalIds.isEmpty()) {
            return 0;
        }

        List<Job> missingJobs = jobRepository.findByActiveTrueAndExternalIdNotIn(seenExternalIds);
        for (Job missingJob : missingJobs) {
            missingJob.deactivate();
        }
        jobRepository.saveAll(missingJobs);
        return missingJobs.size();
    }

    private void markFailed(ScrapeRun scrapeRun, RuntimeException exception) {
        transactionTemplate.executeWithoutResult(_ -> {
            scrapeRun.fail(exception.getMessage());
            scrapeRunRepository.save(scrapeRun);
        });
    }

    private static List<ScrapedJob> validUniqueJobs(Collection<ScrapedJob> scrapedJobs) {
        Map<String, ScrapedJob> jobsByExternalId = new LinkedHashMap<>();
        for (ScrapedJob scrapedJob : scrapedJobs) {
            if (isValid(scrapedJob)) {
                jobsByExternalId.putIfAbsent(scrapedJob.externalId(), scrapedJob);
            }
        }
        return new ArrayList<>(jobsByExternalId.values());
    }

    private static boolean isValid(ScrapedJob scrapedJob) {
        return isPresent(scrapedJob.externalId())
                && isPresent(scrapedJob.title())
                && isPresent(scrapedJob.companyName())
                && isPresent(scrapedJob.sourceUrl());
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private static final class SyncCounters {
        private int created;
        private int updated;
    }
}
