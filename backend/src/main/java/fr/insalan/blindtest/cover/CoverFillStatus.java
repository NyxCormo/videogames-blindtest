package fr.insalan.blindtest.cover;

import java.time.Instant;

public record CoverFillStatus(
    boolean running,
    Instant startedAt,
    Instant finishedAt,
    CoverFillReport lastReport
) {}
