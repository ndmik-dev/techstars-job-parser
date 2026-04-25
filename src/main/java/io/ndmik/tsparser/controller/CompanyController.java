package io.ndmik.tsparser.controller;

import io.ndmik.tsparser.dto.CompanyResponse;
import io.ndmik.tsparser.service.ReferenceDataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final ReferenceDataService referenceDataService;

    public CompanyController(ReferenceDataService referenceDataService) {
        this.referenceDataService = referenceDataService;
    }

    @GetMapping
    public List<CompanyResponse> findCompanies() {
        return referenceDataService.findCompanies();
    }
}
