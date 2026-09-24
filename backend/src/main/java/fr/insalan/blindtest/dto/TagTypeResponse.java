package fr.insalan.blindtest.dto;

import fr.insalan.blindtest.model.TagType;

public record TagTypeResponse(
    Integer id,
    String name
) {
    public static TagTypeResponse from(TagType type) {
        return new TagTypeResponse(type.getId(), type.getName());
    }
}
