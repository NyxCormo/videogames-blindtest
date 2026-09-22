package fr.insalan.blindtest.dto;

public record CreateBlindtestRequest(
    String name,
    int trackCount,
    int difficulty
) {
}
