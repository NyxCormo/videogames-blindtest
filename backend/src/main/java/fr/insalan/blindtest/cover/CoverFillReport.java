package fr.insalan.blindtest.cover;

public record CoverFillReport(
    int scanned,
    int covered,
    int downloaded,
    int notDownloaded
) {}
