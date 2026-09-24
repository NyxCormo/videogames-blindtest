package fr.insalan.blindtest.dto;

public record CreateTagRequest(
    String typeName,
    String tagName
) {
}
