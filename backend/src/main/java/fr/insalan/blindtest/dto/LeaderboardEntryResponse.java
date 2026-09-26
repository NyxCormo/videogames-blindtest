package fr.insalan.blindtest.dto;

import fr.insalan.blindtest.model.BlindtestScore;

public record LeaderboardEntryResponse(
    String listenerName,
    int goodAnswers,
    int franchiseAnswers,
    int bonusAnswers,
    int tracksHeard
) {
    public static LeaderboardEntryResponse from(BlindtestScore score) {
        return new LeaderboardEntryResponse(
            score.getListener().getName(),
            score.getGoodAnswers(),
            score.getFranchiseAnswers(),
            score.getBonusAnswers(),
            score.getTracksHeard()
        );
    }
}
