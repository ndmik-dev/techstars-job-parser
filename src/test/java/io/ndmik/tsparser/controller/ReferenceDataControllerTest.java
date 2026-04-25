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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReferenceDataControllerTest {

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

    @BeforeEach
    void cleanDatabase() {
        jobRepository.deleteAll();
        tagRepository.deleteAll();
        companyRepository.deleteAll();
        scrapeRunRepository.deleteAll();
    }

    @BeforeEach
    void seedReferenceData() {
        Company acme = companyRepository.save(new Company("Acme", "https://jobs.techstars.com/companies/acme"));
        Company beta = companyRepository.save(new Company("Beta", "https://jobs.techstars.com/companies/beta"));
        Tag java = tagRepository.save(new Tag("Java"));
        Tag spring = tagRepository.save(new Tag("Spring"));

        Job job = new Job("job-1", "Backend Engineer", "https://jobs.techstars.com/jobs/job-1", acme);
        job.replaceTags(Set.of(java, spring));
        jobRepository.save(job);

        jobRepository.save(new Job("job-2", "Frontend Engineer", "https://jobs.techstars.com/jobs/job-2", beta));
    }

    @Test
    void returnsCompaniesSortedByName() throws Exception {
        mockMvc.perform(get("/api/companies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Acme"))
                .andExpect(jsonPath("$[0].sourceUrl").value("https://jobs.techstars.com/companies/acme"))
                .andExpect(jsonPath("$[1].name").value("Beta"));
    }

    @Test
    void returnsTagsSortedByName() throws Exception {
        mockMvc.perform(get("/api/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Java"))
                .andExpect(jsonPath("$[1].name").value("Spring"));
    }
}
