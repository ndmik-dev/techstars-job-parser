package io.ndmik.tsparser.controller;

import io.ndmik.tsparser.model.Company;
import io.ndmik.tsparser.model.Job;
import io.ndmik.tsparser.model.Tag;
import io.ndmik.tsparser.repository.CompanyRepository;
import io.ndmik.tsparser.repository.JobRepository;
import io.ndmik.tsparser.repository.ScrapeRunRepository;
import io.ndmik.tsparser.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private ScrapeRunRepository scrapeRunRepository;

    private Long backendJobId;

    @BeforeEach
    void cleanDatabase() {
        jobRepository.deleteAll();
        tagRepository.deleteAll();
        companyRepository.deleteAll();
        scrapeRunRepository.deleteAll();
    }

    @BeforeEach
    void seedJobs() {
        Company acme = companyRepository.save(new Company("Acme", "https://jobs.techstars.com/companies/acme"));
        Company beta = companyRepository.save(new Company("Beta", "https://jobs.techstars.com/companies/beta"));
        Tag java = tagRepository.save(new Tag("Java"));
        Tag spring = tagRepository.save(new Tag("Spring"));
        Tag react = tagRepository.save(new Tag("React"));

        Job backendJob = job(
                "job-1",
                "Backend Engineer",
                "Remote",
                "remote",
                "Mid-Senior Level",
                acme,
                Set.of(java, spring)
        );
        backendJobId = jobRepository.save(backendJob).getId();

        Job frontendJob = job("job-2", "Frontend Engineer", "London", null, null, beta, Set.of(react));
        frontendJob.deactivate();
        jobRepository.save(frontendJob);
    }

    @Test
    void returnsPagedActiveJobsByDefault() throws Exception {
        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Backend Engineer"))
                .andExpect(jsonPath("$.content[0].companyName").value("Acme"))
                .andExpect(jsonPath("$.content[0].tags", containsInAnyOrder("Java", "Spring")));
    }

    @Test
    void filtersJobsByCompanyTagLocationAndQuery() throws Exception {
        mockMvc.perform(get("/api/jobs")
                        .param("company", "acme")
                        .param("tag", "spring")
                        .param("location", "remote")
                        .param("q", "backend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].externalId").value("job-1"));
    }

    @Test
    void querySearchesTagsToo() throws Exception {
        mockMvc.perform(get("/api/jobs").param("q", "java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].externalId").value("job-1"));
    }

    @Test
    void canReturnInactiveJobsWhenRequested() throws Exception {
        mockMvc.perform(get("/api/jobs").param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].externalId").value("job-2"));
    }

    @Test
    void returnsJobDetails() throws Exception {
        mockMvc.perform(get("/api/jobs/{id}", backendJobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(backendJobId))
                .andExpect(jsonPath("$.title").value("Backend Engineer"))
                .andExpect(jsonPath("$.companyName").value("Acme"));
    }

    @Test
    void returnsNotFoundForMissingJob() throws Exception {
        mockMvc.perform(get("/api/jobs/{id}", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Job not found"));
    }

    private static Job job(String externalId,
                           String title,
                           String location,
                           String remoteType,
                           String seniority,
                           Company company,
                           Set<Tag> tags) {
        String sourceUrl = "https://jobs.techstars.com/jobs/" + externalId;
        Job job = new Job(externalId, title, sourceUrl, company);
        job.updateDetails(
                title,
                location,
                title + " at " + company.getName(),
                sourceUrl,
                remoteType,
                seniority,
                null,
                null,
                company
        );
        job.replaceTags(tags);
        return job;
    }
}
