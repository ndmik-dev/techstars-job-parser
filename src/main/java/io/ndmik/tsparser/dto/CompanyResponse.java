package io.ndmik.tsparser.dto;

import io.ndmik.tsparser.model.Company;

public record CompanyResponse(
        Long id,
        String name,
        String sourceUrl
) {

    public static CompanyResponse from(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getSourceUrl()
        );
    }
}
