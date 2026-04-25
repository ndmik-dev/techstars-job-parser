package io.ndmik.tsparser.service;

import io.ndmik.tsparser.config.TechstarsScraperProperties;
import io.ndmik.tsparser.dto.ScrapedJob;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class TechstarsJobsClient {

    private final TechstarsScraperProperties properties;
    private final TechstarsJobsParser parser;

    public TechstarsJobsClient(TechstarsScraperProperties properties, TechstarsJobsParser parser) {
        this.properties = properties;
        this.parser = parser;
    }

    public List<ScrapedJob> fetchJobs() {
        try {
            Document document = Jsoup.connect(properties.jobsUrl())
                    .userAgent(properties.userAgent())
                    .timeout((int) properties.timeout().toMillis())
                    .get();
            return parser.parse(document, properties.jobsUrl());
        } catch (IOException exception) {
            throw new JobScrapingException("Failed to fetch jobs from Techstars", exception);
        }
    }
}
