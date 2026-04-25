package io.ndmik.tsparser.dto;

import io.ndmik.tsparser.model.Job;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public record JobResponse(
        Long id,
        String externalId,
        String title,
        String companyName,
        String companyUrl,
        String location,
        String description,
        String sourceUrl,
        String remoteType,
        String seniority,
        String salaryText,
        String postedAtText,
        boolean active,
        Instant firstSeenAt,
        Instant lastSeenAt,
        List<String> tags
) {

    public static JobResponse from(Job job) {
        return new JobResponse(
                job.getId(),
                job.getExternalId(),
                job.getTitle(),
                job.getCompany().getName(),
                job.getCompany().getSourceUrl(),
                job.getLocation(),
                job.getDescription(),
                job.getSourceUrl(),
                job.getRemoteType(),
                job.getSeniority(),
                job.getSalaryText(),
                job.getPostedAtText(),
                job.isActive(),
                job.getFirstSeenAt(),
                job.getLastSeenAt(),
                job.getTags().stream()
                        .map(tag -> tag.getName())
                        .sorted(Comparator.naturalOrder())
                        .toList()
        );
    }
}
