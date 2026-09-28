package fr.insalan.blindtest.dto;

public record CreateGameRequest(
    String name,
    Integer franchiseId
) {
}
