package fr.insalan.blindtest.dto;

public record StatsResponse(
    long tracks,
    long playableTracks,
    long games,
    long franchises,
    long listeners,
    long votes
) {
}
