package io.ndmik.tsparser.service;

import io.ndmik.tsparser.dto.ScrapedJob;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TechstarsJobsParserTest {

    private final TechstarsJobsParser parser = new TechstarsJobsParser();

    @Test
    void parsesJobCardsFromTechstarsHtml() {
        String html = """
                <div data-testid="job-list-item">
                  <meta itemprop="description" content="AI Product Manager at Inspektlabs"/>
                  <a href="/companies/inspektlabs/jobs/76204857-ai-product-manager#content" data-testid="job-title-link">
                    <div itemprop="title">AI Product Manager</div>
                  </a>
                  <div itemprop="hiringOrganization">
                    <a data-testid="link" href="/companies/inspektlabs#content">Inspektlabs</a>
                  </div>
                  <div itemprop="jobLocation">
                    <div itemprop="address">
                      <meta itemprop="addressLocality" content="New Delhi, Delhi, India"/>
                    </div>
                  </div>
                  <div class="added">
                    <div>Today<meta itemprop="datePosted" content="2026-04-25"/></div>
                  </div>
                  <div data-testid="tag"><div>Software</div></div>
                  <div data-testid="tag"><div>Software</div></div>
                  <div data-testid="tag"><div>Mid-Senior Level</div></div>
                </div>
                """;

        List<ScrapedJob> jobs = parser.parse(html, "https://jobs.techstars.com/jobs");

        assertThat(jobs).hasSize(1);
        ScrapedJob job = jobs.getFirst();
        assertThat(job.externalId()).isEqualTo("76204857-ai-product-manager");
        assertThat(job.title()).isEqualTo("AI Product Manager");
        assertThat(job.companyName()).isEqualTo("Inspektlabs");
        assertThat(job.companyUrl()).isEqualTo("https://jobs.techstars.com/companies/inspektlabs#content");
        assertThat(job.location()).isEqualTo("New Delhi, Delhi, India");
        assertThat(job.description()).isEqualTo("AI Product Manager at Inspektlabs");
        assertThat(job.sourceUrl()).isEqualTo("https://jobs.techstars.com/companies/inspektlabs/jobs/76204857-ai-product-manager#content");
        assertThat(job.postedAtText()).isEqualTo("Today");
        assertThat(job.seniority()).isEqualTo("Mid-Senior Level");
        assertThat(job.tags()).containsExactly("Software", "Mid-Senior Level");
    }

    @Test
    void skipsCardsWithoutJobTitleLink() {
        String html = """
                <div data-testid="job-list-item">
                  <div>Not a valid job card</div>
                </div>
                """;

        assertThat(parser.parse(html, "https://jobs.techstars.com/jobs")).isEmpty();
    }
}
