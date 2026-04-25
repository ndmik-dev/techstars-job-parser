package io.ndmik.tsparser.repository;

import io.ndmik.tsparser.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByName(String name);

    Optional<Company> findByNameIgnoreCase(String name);
}
