package fr.insalan.blindtest.dto;

import fr.insalan.blindtest.cover.CoverUrls;
import fr.insalan.blindtest.model.Track;

public record DiscoverTrackResponse(
    Integer trackId,
    String audioLink,
    String franchiseName,
    Integer gameId,
    String gameName,
    String trackName,
    boolean finished,
    long voted,
    int remaining,
    String coverUrl
) {
    public static DiscoverTrackResponse allDiscovered(long voted) {
        return new DiscoverTrackResponse(null, null, null, null, null, null, true, voted, 0, null);
    }

    public static DiscoverTrackResponse from(Track track, long voted, int remaining) {
        return new DiscoverTrackResponse(
            track.getId(),
            track.getAudioLink(),
            track.getGame().getFranchise().getName(),
            track.getGame().getId(),
            track.getGame().getName(),
            track.getName(),
            false,
            voted,
            remaining,
            CoverUrls.publicUrl(track.getGame())
        );
    }
}
