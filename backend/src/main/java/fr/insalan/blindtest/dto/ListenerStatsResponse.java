package fr.insalan.blindtest.dto;

public record ListenerStatsResponse(
    long votes,
    long known,
    long remaining
) {
}
