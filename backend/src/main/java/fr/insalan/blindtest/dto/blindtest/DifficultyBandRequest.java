package fr.insalan.blindtest.dto.blindtest;

public record DifficultyBandRequest(
    int minDifficulty,
    int maxDifficulty,
    int proportion
) {
}
