package io.ndmik.tsparser.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "scrape_runs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScrapeRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ScrapeRunStatus status;

    @Column(name = "retrieved_count", nullable = false)
    private int retrievedCount;

    @Column(name = "created_count", nullable = false)
    private int createdCount;

    @Column(name = "updated_count", nullable = false)
    private int updatedCount;

    @Column(name = "deactivated_count", nullable = false)
    private int deactivatedCount;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    public static ScrapeRun started() {
        ScrapeRun scrapeRun = new ScrapeRun();
        scrapeRun.startedAt = Instant.now();
        scrapeRun.status = ScrapeRunStatus.RUNNING;
        return scrapeRun;
    }

    public void complete(int retrievedCount,
                         int createdCount,
                         int updatedCount,
        int deactivatedCount) {
        this.status = ScrapeRunStatus.COMPLETED;
        this.finishedAt = Instant.now();
        this.retrievedCount = retrievedCount;
        this.createdCount = createdCount;
        this.updatedCount = updatedCount;
        this.deactivatedCount = deactivatedCount;
        this.errorMessage = null;
    }

    public void fail(String errorMessage) {
        this.status = ScrapeRunStatus.FAILED;
        this.finishedAt = Instant.now();
        this.errorMessage = errorMessage;
    }
}
