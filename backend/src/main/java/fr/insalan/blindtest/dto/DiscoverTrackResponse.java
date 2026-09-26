package fr.insalan.blindtest.dto;

public record DiscoverTrackResponse(
    Integer trackId,
    String audioLink,
    boolean finished
) {
}
