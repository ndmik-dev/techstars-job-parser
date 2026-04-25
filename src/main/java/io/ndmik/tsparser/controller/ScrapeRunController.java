package io.ndmik.tsparser.controller;

import io.ndmik.tsparser.dto.JobSyncResult;
import io.ndmik.tsparser.dto.PageResponse;
import io.ndmik.tsparser.dto.ScrapeRunResponse;
import io.ndmik.tsparser.repository.ScrapeRunRepository;
import io.ndmik.tsparser.service.ScrapeService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/scrape-runs")
public class ScrapeRunController {

    private final ScrapeService scrapeService;
    private final ScrapeRunRepository scrapeRunRepository;

    public ScrapeRunController(ScrapeService scrapeService,
                               ScrapeRunRepository scrapeRunRepository) {
        this.scrapeService = scrapeService;
        this.scrapeRunRepository = scrapeRunRepository;
    }

    @PostMapping
    public ResponseEntity<ScrapeRunResponse> startScrape() {
        JobSyncResult result = scrapeService.scrapeAndSync();
        ScrapeRunResponse response = scrapeRunRepository.findById(result.scrapeRunId())
                .map(ScrapeRunResponse::from)
                .orElseThrow();

        return ResponseEntity
                .created(URI.create("/api/scrape-runs/" + response.id()))
                .body(response);
    }

    @GetMapping
    public PageResponse<ScrapeRunResponse> findScrapeRuns(@PageableDefault(size = 20, sort = "startedAt") Pageable pageable) {
        return PageResponse.from(scrapeRunRepository.findAll(pageable)
                .map(ScrapeRunResponse::from));
    }
}
