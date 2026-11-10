package fr.insalan.blindtest.youtube;

public record YoutubeAudioFailure(
    Integer trackId,
    String trackName,
    String gameName,
    String youtubeLink,
    String error
) {}
