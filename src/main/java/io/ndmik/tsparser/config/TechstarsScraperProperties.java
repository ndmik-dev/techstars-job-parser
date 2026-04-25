package io.ndmik.tsparser.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "techstars.scraper")
public record TechstarsScraperProperties(
        String jobsUrl,
        Duration timeout,
        String userAgent
) {
}
