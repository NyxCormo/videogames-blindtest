package fr.insalan.blindtest.dto;

import fr.insalan.blindtest.tag.TagUsage;

public record TagUsageResponse(
    Integer id,
    String name,
    String typeName,
    long count
) {
    public static TagUsageResponse from(TagUsage usage) {
        return new TagUsageResponse(
            usage.tag().getId(),
            usage.tag().getName(),
            usage.tag().getType().getName(),
            usage.count()
        );
    }
}
