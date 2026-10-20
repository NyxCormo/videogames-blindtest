package fr.insalan.blindtest.cover;

public record CoverFillReport(
    int checked,
    int found,
    int downloaded,
    int failed
) {}
