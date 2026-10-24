package fr.insalan.blindtest.youtube;

public record YoutubeAudioReport(
    int done,
    int toDownload,
    int downloaded,
    int failed
) {}
