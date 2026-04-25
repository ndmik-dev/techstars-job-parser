package io.ndmik.tsparser.controller;

import io.ndmik.tsparser.dto.ScrapedJob;
import io.ndmik.tsparser.repository.CompanyRepository;
import io.ndmik.tsparser.repository.JobRepository;
import io.ndmik.tsparser.repository.ScrapeRunRepository;
import io.ndmik.tsparser.repository.TagRepository;
import io.ndmik.tsparser.service.TechstarsJobsClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ScrapeRunControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeTechstarsJobsClient jobsClient;

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
    void startsScrapeAndReturnsCreatedRun() throws Exception {
        jobsClient.returnJobs(List.of(scrapedJob("job-1")));

        mockMvc.perform(post("/api/scrape-runs"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/scrape-runs/")))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.retrievedCount").value(1))
                .andExpect(jsonPath("$.createdCount").value(1));
    }

    @Test
    void returnsScrapeRunHistory() throws Exception {
        jobsClient.returnJobs(List.of(scrapedJob("job-1")));
        mockMvc.perform(post("/api/scrape-runs")).andExpect(status().isCreated());

        mockMvc.perform(get("/api/scrape-runs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status").value("COMPLETED"));
    }

    private static ScrapedJob scrapedJob(String externalId) {
        return new ScrapedJob(
                externalId,
                "Backend Engineer",
                "Acme",
                "https://jobs.techstars.com/companies/acme",
                "Remote",
                "Backend Engineer at Acme",
                "https://jobs.techstars.com/jobs/" + externalId,
                "remote",
                "Mid-Senior Level",
                null,
                "Today",
                List.of("Java", "Spring")
        );
    }

    @TestConfiguration
    static class FakeClientConfiguration {

        @Bean
        @Primary
        FakeTechstarsJobsClient fakeTechstarsJobsClient() {
            return new FakeTechstarsJobsClient();
        }
    }

    static class FakeTechstarsJobsClient extends TechstarsJobsClient {

        private List<ScrapedJob> jobs = List.of();

        FakeTechstarsJobsClient() {
            super(null, null);
        }

        @Override
        public List<ScrapedJob> fetchJobs() {
            return jobs;
        }

        void returnJobs(List<ScrapedJob> jobs) {
            this.jobs = jobs;
        }
    }
}
