package fr.insalan.blindtest.cover;

public record CoverFillReport(
    int scanned,
    int toScan,
    int covered,
    int downloaded,
    int notDownloaded
) {}
