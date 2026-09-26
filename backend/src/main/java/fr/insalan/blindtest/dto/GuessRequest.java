package fr.insalan.blindtest.dto;

public record GuessRequest(
    Integer gameId,
    Integer trackId
) {
}
