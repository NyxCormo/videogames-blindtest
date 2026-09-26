package fr.insalan.blindtest.dto;

import fr.insalan.blindtest.model.Track;

public record DiscoverTrackResponse(
    Integer trackId,
    String audioLink,
    String franchiseName,
    String gameName,
    String trackName,
    boolean finished
) {
    public static DiscoverTrackResponse allDiscovered() {
        return new DiscoverTrackResponse(null, null, null, null, null, true);
    }

    public static DiscoverTrackResponse from(Track track) {
        return new DiscoverTrackResponse(
            track.getId(),
            track.getAudioLink(),
            track.getGame().getFranchise().getName(),
            track.getGame().getName(),
            track.getName(),
            false
        );
    }
}
