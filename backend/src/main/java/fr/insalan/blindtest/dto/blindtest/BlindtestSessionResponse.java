package fr.insalan.blindtest.dto.blindtest;

public record BlindtestSessionResponse(
    Integer trackId,
    String audioLink,
    boolean finished,
    int tracksHeard,
    int totalTracks,
    int goodAnswers,
    int attemptsRemaining,
    int maxAttempts,
    Integer foundFranchiseId,
    int franchiseAnswers,
    int bonusAnswers
) {
}
