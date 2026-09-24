package fr.insalan.blindtest.dto;

import fr.insalan.blindtest.model.Tag;

public record TagResponse(
    Integer id,
    String name,
    String typeName
) {
    public static TagResponse from(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName(), tag.getType().getName());
    }
}
