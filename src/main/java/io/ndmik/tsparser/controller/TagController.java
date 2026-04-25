package io.ndmik.tsparser.controller;

import io.ndmik.tsparser.dto.TagResponse;
import io.ndmik.tsparser.service.ReferenceDataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final ReferenceDataService referenceDataService;

    public TagController(ReferenceDataService referenceDataService) {
        this.referenceDataService = referenceDataService;
    }

    @GetMapping
    public List<TagResponse> findTags() {
        return referenceDataService.findTags();
    }
}
