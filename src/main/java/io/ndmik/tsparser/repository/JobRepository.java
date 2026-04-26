package io.ndmik.tsparser.repository;

import io.ndmik.tsparser.model.Job;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    @EntityGraph(attributePaths = {"company", "tags"})
    Optional<Job> findByExternalId(String externalId);

    @EntityGraph(attributePaths = {"company", "tags"})
    Optional<Job> findWithCompanyAndTagsById(Long id);

    List<Job> findByActiveTrueAndExternalIdNotIn(Collection<String> externalIds);
}
