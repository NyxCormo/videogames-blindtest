package fr.insalan.blindtest.dto;

public record GuessResponse(
    boolean correct,
    boolean bonusCorrect,
    RevealResponse reveal
) {
}
