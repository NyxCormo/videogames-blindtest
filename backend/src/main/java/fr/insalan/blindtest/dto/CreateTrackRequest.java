package fr.insalan.blindtest.dto;

public record CreateTrackRequest(
    String name,
    Integer gameId
) {
}
