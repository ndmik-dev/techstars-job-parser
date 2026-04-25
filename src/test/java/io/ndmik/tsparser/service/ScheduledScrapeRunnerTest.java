package io.ndmik.tsparser.service;

import io.ndmik.tsparser.dto.JobSyncResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduledScrapeRunnerTest {

    @Test
    void runsScrapeService() {
        FakeScrapeService scrapeService = new FakeScrapeService();
        ScheduledScrapeRunner runner = new ScheduledScrapeRunner(scrapeService);

        runner.runScheduledScrape();

        assertThat(scrapeService.calls).isEqualTo(1);
    }

    @Test
    void skipsWhenScrapeIsAlreadyRunning() {
        FakeScrapeService scrapeService = new FakeScrapeService();
        scrapeService.failure = new ScrapeAlreadyRunningException();
        ScheduledScrapeRunner runner = new ScheduledScrapeRunner(scrapeService);

        runner.runScheduledScrape();

        assertThat(scrapeService.calls).isEqualTo(1);
    }

    private static class FakeScrapeService extends ScrapeService {

        private int calls;
        private RuntimeException failure;

        FakeScrapeService() {
            super(null, null);
        }

        @Override
        public JobSyncResult scrapeAndSync() {
            calls++;
            if (failure != null) {
                throw failure;
            }
            return new JobSyncResult(1L, 3, 1, 2, 0);
        }
    }
}
