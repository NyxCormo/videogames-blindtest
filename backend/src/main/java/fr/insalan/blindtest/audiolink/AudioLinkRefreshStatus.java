package fr.insalan.blindtest.audiolink;

import java.time.Instant;

public record AudioLinkRefreshStatus(
    boolean running,
    Instant startedAt,
    Instant finishedAt,
    AudioLinkRefreshReport lastReport
) {}
