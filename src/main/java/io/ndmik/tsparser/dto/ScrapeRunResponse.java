package io.ndmik.tsparser.dto;

import io.ndmik.tsparser.model.ScrapeRun;
import io.ndmik.tsparser.model.ScrapeRunStatus;

import java.time.Instant;

public record ScrapeRunResponse(
        Long id,
        Instant startedAt,
        Instant finishedAt,
        ScrapeRunStatus status,
        int retrievedCount,
        int createdCount,
        int updatedCount,
        int deactivatedCount,
        String errorMessage
) {

    public static ScrapeRunResponse from(ScrapeRun scrapeRun) {
        return new ScrapeRunResponse(
                scrapeRun.getId(),
                scrapeRun.getStartedAt(),
                scrapeRun.getFinishedAt(),
                scrapeRun.getStatus(),
                scrapeRun.getRetrievedCount(),
                scrapeRun.getCreatedCount(),
                scrapeRun.getUpdatedCount(),
                scrapeRun.getDeactivatedCount(),
                scrapeRun.getErrorMessage()
        );
    }
}
