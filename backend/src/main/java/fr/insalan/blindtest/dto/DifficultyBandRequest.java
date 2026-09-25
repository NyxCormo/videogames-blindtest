package fr.insalan.blindtest.dto;

public record DifficultyBandRequest(
    int minDifficulty,
    int maxDifficulty,
    int proportion
) {
}
