package io.ndmik.tsparser.service;

import io.ndmik.tsparser.dto.JobSyncResult;
import io.ndmik.tsparser.dto.ScrapedJob;
import io.ndmik.tsparser.model.Job;
import io.ndmik.tsparser.model.ScrapeRunStatus;
import io.ndmik.tsparser.repository.CompanyRepository;
import io.ndmik.tsparser.repository.JobRepository;
import io.ndmik.tsparser.repository.ScrapeRunRepository;
import io.ndmik.tsparser.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JobSyncServiceTest {

    @Autowired
    private JobSyncService jobSyncService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private ScrapeRunRepository scrapeRunRepository;

    @BeforeEach
    void cleanDatabase() {
        jobRepository.deleteAll();
        tagRepository.deleteAll();
        companyRepository.deleteAll();
        scrapeRunRepository.deleteAll();
    }

    @Test
    void createsJobsCompaniesTagsAndScrapeRun() {
        ScrapedJob scrapedJob = scrapedJob("job-1", "Backend Engineer", "Acme", "Remote", List.of("Java", "Java", "Spring"));

        JobSyncResult result = jobSyncService.sync(List.of(scrapedJob));

        assertThat(result.retrievedCount()).isEqualTo(1);
        assertThat(result.createdCount()).isEqualTo(1);
        assertThat(result.updatedCount()).isZero();
        assertThat(result.deactivatedCount()).isZero();
        assertThat(jobRepository.findAll()).hasSize(1);
        assertThat(companyRepository.findAll()).hasSize(1);
        assertThat(tagRepository.findAll()).hasSize(2);
        assertThat(scrapeRunRepository.findById(result.scrapeRunId()))
                .hasValueSatisfying(run -> assertThat(run.getStatus()).isEqualTo(ScrapeRunStatus.COMPLETED));

        Job job = jobRepository.findByExternalId("job-1").orElseThrow();
        assertThat(job.getTitle()).isEqualTo("Backend Engineer");
        assertThat(job.getCompany().getName()).isEqualTo("Acme");
        assertThat(job.getTags()).extracting("name").containsExactlyInAnyOrder("Java", "Spring");
    }

    @Test
    void updatesExistingJobAndDeactivatesMissingJob() {
        jobSyncService.sync(List.of(
                scrapedJob("job-1", "Backend Engineer", "Acme", "Remote", List.of("Java")),
                scrapedJob("job-2", "Frontend Engineer", "Beta", "London", List.of("React"))
        ));

        JobSyncResult result = jobSyncService.sync(List.of(
                scrapedJob("job-1", "Senior Backend Engineer", "Acme", "Berlin", List.of("Java", "Spring"))
        ));

        assertThat(result.retrievedCount()).isEqualTo(1);
        assertThat(result.createdCount()).isZero();
        assertThat(result.updatedCount()).isEqualTo(1);
        assertThat(result.deactivatedCount()).isEqualTo(1);

        Job updatedJob = jobRepository.findByExternalId("job-1").orElseThrow();
        assertThat(updatedJob.getTitle()).isEqualTo("Senior Backend Engineer");
        assertThat(updatedJob.getLocation()).isEqualTo("Berlin");
        assertThat(updatedJob.isActive()).isTrue();
        assertThat(updatedJob.getTags()).extracting("name").containsExactlyInAnyOrder("Java", "Spring");

        Job deactivatedJob = jobRepository.findByExternalId("job-2").orElseThrow();
        assertThat(deactivatedJob.isActive()).isFalse();
    }

    @Test
    void ignoresInvalidAndDuplicateScrapedJobs() {
        JobSyncResult result = jobSyncService.sync(List.of(
                scrapedJob("job-1", "Backend Engineer", "Acme", "Remote", List.of("Java")),
                scrapedJob("job-1", "Backend Engineer Duplicate", "Acme", "Remote", List.of("Spring")),
                scrapedJob("", "Missing External Id", "Acme", "Remote", List.of("Java")),
                scrapedJob("job-2", "", "Acme", "Remote", List.of("Java")),
                scrapedJob("job-3", "Missing Company", "", "Remote", List.of("Java"))
        ));

        assertThat(result.retrievedCount()).isEqualTo(1);
        assertThat(result.createdCount()).isEqualTo(1);
        assertThat(result.updatedCount()).isZero();
        assertThat(jobRepository.findAll()).hasSize(1);

        Job job = jobRepository.findByExternalId("job-1").orElseThrow();
        assertThat(job.getTitle()).isEqualTo("Backend Engineer");
        assertThat(job.getTags()).extracting("name").containsExactly("Java");
    }

    @Test
    void doesNotDeactivateExistingJobsWhenScrapeResultIsEmpty() {
        jobSyncService.sync(List.of(scrapedJob("job-1", "Backend Engineer", "Acme", "Remote", List.of("Java"))));

        JobSyncResult result = jobSyncService.sync(List.of());

        assertThat(result.retrievedCount()).isZero();
        assertThat(result.deactivatedCount()).isZero();
        assertThat(jobRepository.findByExternalId("job-1")).hasValueSatisfying(job -> assertThat(job.isActive()).isTrue());
    }

    private static ScrapedJob scrapedJob(String externalId,
                                         String title,
                                         String companyName,
                                         String location,
                                         List<String> tags) {
        return new ScrapedJob(
                externalId,
                title,
                companyName,
                "https://jobs.techstars.com/companies/" + companyName.toLowerCase(),
                location,
                title + " at " + companyName,
                "https://jobs.techstars.com/jobs/" + externalId,
                "remote",
                null,
                null,
                "Today",
                tags
        );
    }
}
