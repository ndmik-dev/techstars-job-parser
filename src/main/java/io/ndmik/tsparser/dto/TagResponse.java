package io.ndmik.tsparser.dto;

import io.ndmik.tsparser.model.Tag;

public record TagResponse(
        Long id,
        String name
) {

    public static TagResponse from(Tag tag) {
        return new TagResponse(
                tag.getId(),
                tag.getName()
        );
    }
}
