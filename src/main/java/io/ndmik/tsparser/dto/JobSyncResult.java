package io.ndmik.tsparser.dto;

public record JobSyncResult(
        long scrapeRunId,
        int retrievedCount,
        int createdCount,
        int updatedCount,
        int deactivatedCount
) {
}
