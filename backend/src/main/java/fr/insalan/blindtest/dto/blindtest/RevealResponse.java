package fr.insalan.blindtest.dto.blindtest;

import fr.insalan.blindtest.cover.CoverUrls;
import fr.insalan.blindtest.model.Track;

public record RevealResponse(
    Integer trackId,
    String franchiseName,
    Integer gameId,
    String gameName,
    String trackName,
    String coverUrl
) {
    public static RevealResponse from(Track track) {
        return new RevealResponse(
            track.getId(),
            track.getGame().getFranchise().getName(),
            track.getGame().getId(),
            track.getGame().getName(),
            track.getName(),
            CoverUrls.publicUrl(track.getGame())
        );
    }
}
