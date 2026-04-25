package io.ndmik.tsparser.service;

import io.ndmik.tsparser.dto.JobSyncResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "techstars.scraper", name = "scheduling-enabled", havingValue = "true")
public class ScheduledScrapeRunner {

    private static final Logger log = LoggerFactory.getLogger(ScheduledScrapeRunner.class);

    private final ScrapeService scrapeService;

    public ScheduledScrapeRunner(ScrapeService scrapeService) {
        this.scrapeService = scrapeService;
    }

    @Scheduled(cron = "${techstars.scraper.cron}")
    public void runScheduledScrape() {
        try {
            JobSyncResult result = scrapeService.scrapeAndSync();
            log.info(
                    "Scheduled scrape completed: scrapeRunId={}, retrieved={}, created={}, updated={}, deactivated={}",
                    result.scrapeRunId(),
                    result.retrievedCount(),
                    result.createdCount(),
                    result.updatedCount(),
                    result.deactivatedCount()
            );
        } catch (ScrapeAlreadyRunningException exception) {
            log.info("Scheduled scrape skipped because another scrape is already running");
        } catch (RuntimeException exception) {
            log.error("Scheduled scrape failed", exception);
        }
    }
}
