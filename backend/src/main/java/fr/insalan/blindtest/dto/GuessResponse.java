package fr.insalan.blindtest.dto;

public record GuessResponse(
    boolean correct,
    RevealResponse reveal
) {
}
