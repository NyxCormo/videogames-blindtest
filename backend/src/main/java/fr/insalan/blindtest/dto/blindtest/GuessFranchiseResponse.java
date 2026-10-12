package fr.insalan.blindtest.dto.blindtest;

public record GuessFranchiseResponse(
    boolean correct,
    RevealResponse reveal
) {
}
