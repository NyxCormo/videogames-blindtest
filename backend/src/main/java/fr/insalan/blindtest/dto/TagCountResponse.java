package fr.insalan.blindtest.dto;

import fr.insalan.blindtest.repository.TagWithCount;

public record TagCountResponse(
    Integer id,
    String name,
    String typeName,
    long tracks
) {
    public static TagCountResponse from(TagWithCount tag) {
        return new TagCountResponse(tag.getId(), tag.getName(), tag.getTypeName(), tag.getTracks());
    }
}
