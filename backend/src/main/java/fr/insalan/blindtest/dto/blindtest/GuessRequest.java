package fr.insalan.blindtest.dto.blindtest;

public record GuessRequest(
    Integer gameId,
    Integer trackId
) {
}
