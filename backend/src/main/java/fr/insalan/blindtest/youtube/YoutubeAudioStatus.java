package fr.insalan.blindtest.youtube;

import java.time.Instant;
import java.util.List;

public record YoutubeAudioStatus(
    boolean available,
    boolean running,
    Instant startedAt,
    Instant finishedAt,
    YoutubeAudioReport lastReport,
    long tracks,
    long downloaded,
    List<YoutubeAudioFailure> failures
) {}
