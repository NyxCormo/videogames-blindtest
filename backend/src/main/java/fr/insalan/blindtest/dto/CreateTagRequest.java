package fr.insalan.blindtest.dto;

public record CreateTagRequest(
    Integer typeId,
    String tagName
) {
}
