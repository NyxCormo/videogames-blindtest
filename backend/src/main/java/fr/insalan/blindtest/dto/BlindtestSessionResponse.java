package fr.insalan.blindtest.dto;

public record BlindtestSessionResponse(
    Integer trackId,
    String audioLink,
    boolean finished,
    int tracksHeard,
    int totalTracks,
    int goodAnswers,
    int attemptsRemaining
) {
}
