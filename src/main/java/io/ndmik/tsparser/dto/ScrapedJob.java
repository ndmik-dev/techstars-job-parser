package io.ndmik.tsparser.dto;

import java.util.List;

public record ScrapedJob(
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
        List<String> tags
) {
}
