package io.ndmik.tsparser.dto;

public record JobFilter(
        String q,
        String location,
        String company,
        String tag,
        String remoteType,
        String seniority,
        Boolean active
) {
}
