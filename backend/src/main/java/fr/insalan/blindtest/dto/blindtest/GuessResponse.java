package fr.insalan.blindtest.dto.blindtest;

public record GuessResponse(
    boolean correct,
    boolean bonusCorrect,
    RevealResponse reveal
) {
}
