package io.ndmik.tsparser.service;

import io.ndmik.tsparser.dto.CompanyResponse;
import io.ndmik.tsparser.dto.TagResponse;
import io.ndmik.tsparser.repository.CompanyRepository;
import io.ndmik.tsparser.repository.TagRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReferenceDataService {

    private static final Sort NAME_ASC = Sort.by("name").ascending();

    private final CompanyRepository companyRepository;
    private final TagRepository tagRepository;

    public ReferenceDataService(CompanyRepository companyRepository,
                                TagRepository tagRepository) {
        this.companyRepository = companyRepository;
        this.tagRepository = tagRepository;
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> findCompanies() {
        return companyRepository.findAll(NAME_ASC).stream()
                .map(CompanyResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TagResponse> findTags() {
        return tagRepository.findAll(NAME_ASC).stream()
                .map(TagResponse::from)
                .toList();
    }
}
