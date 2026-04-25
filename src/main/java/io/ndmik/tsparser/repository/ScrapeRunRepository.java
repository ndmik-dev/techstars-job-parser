package io.ndmik.tsparser.repository;

import io.ndmik.tsparser.model.ScrapeRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ScrapeRunRepository extends JpaRepository<ScrapeRun, Long> {

    Optional<ScrapeRun> findTopByOrderByStartedAtDesc();
}
