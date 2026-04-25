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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

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
        return jobRepository.findByExternalId(scrapedJob.externalId())
                .map(existingJob -> {
                    counters.updated++;
                    return existingJob;
                })
                .orElseGet(() -> {
                    counters.created++;
                    return new Job(
                            scrapedJob.externalId(),
                            scrapedJob.title(),
                            scrapedJob.sourceUrl(),
                            company
                    );
                });
    }

    private Company resolveCompany(ScrapedJob scrapedJob) {
        return companyRepository.findByNameIgnoreCase(scrapedJob.companyName())
                .map(company -> {
                    setIfChanged(company.getSourceUrl(), scrapedJob.companyUrl(), company::setSourceUrl);
                    return company;
                })
                .orElseGet(() -> companyRepository.save(new Company(scrapedJob.companyName(), scrapedJob.companyUrl())));
    }

    private Set<Tag> resolveTags(List<String> tagNames) {
        Set<Tag> tags = new HashSet<>();
        for (String tagName : nullSafeList(tagNames)) {
            if (tagName == null || tagName.isBlank()) {
                continue;
            }
            tags.add(tagRepository.findByNameIgnoreCase(tagName)
                    .orElseGet(() -> tagRepository.save(new Tag(tagName))));
        }
        return tags;
    }

    private void applyScrapedData(Job job, ScrapedJob scrapedJob, Company company, Set<Tag> tags) {
        setIfChanged(job.getTitle(), scrapedJob.title(), job::setTitle);
        setIfChanged(job.getLocation(), scrapedJob.location(), job::setLocation);
        setIfChanged(job.getDescription(), scrapedJob.description(), job::setDescription);
        setIfChanged(job.getSourceUrl(), scrapedJob.sourceUrl(), job::setSourceUrl);
        setIfChanged(job.getRemoteType(), scrapedJob.remoteType(), job::setRemoteType);
        setIfChanged(job.getSeniority(), scrapedJob.seniority(), job::setSeniority);
        setIfChanged(job.getSalaryText(), scrapedJob.salaryText(), job::setSalaryText);
        setIfChanged(job.getPostedAtText(), scrapedJob.postedAtText(), job::setPostedAtText);
        job.setCompany(company);
        job.setActive(true);
        job.setLastSeenAt(Instant.now());
        job.replaceTags(tags);
    }

    private int deactivateMissingJobs(Set<String> seenExternalIds) {
        if (seenExternalIds.isEmpty()) {
            return 0;
        }

        List<Job> missingJobs = jobRepository.findByActiveTrueAndExternalIdNotIn(seenExternalIds);
        for (Job missingJob : missingJobs) {
            missingJob.setActive(false);
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
        for (ScrapedJob scrapedJob : nullSafeCollection(scrapedJobs)) {
            if (isValid(scrapedJob)) {
                jobsByExternalId.putIfAbsent(scrapedJob.externalId(), scrapedJob);
            }
        }
        return new ArrayList<>(jobsByExternalId.values());
    }

    private static boolean isValid(ScrapedJob scrapedJob) {
        return scrapedJob != null
                && isPresent(scrapedJob.externalId())
                && isPresent(scrapedJob.title())
                && isPresent(scrapedJob.companyName())
                && isPresent(scrapedJob.sourceUrl());
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private static <T> Collection<T> nullSafeCollection(Collection<T> values) {
        return values == null
                ? List.of()
                : values;
    }

    private static <T> List<T> nullSafeList(List<T> values) {
        return values == null
                ? List.of()
                : values;
    }

    private static void setIfChanged(String currentValue, String newValue, Consumer<String> setter) {
        if (!Objects.equals(currentValue, newValue)) {
            setter.accept(newValue);
        }
    }

    private static final class SyncCounters {
        private int created;
        private int updated;
    }
}
