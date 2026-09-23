package fr.insalan.blindtest.dto;

import fr.insalan.blindtest.model.Track;

public record RevealResponse(
    Integer trackId,
    String franchiseName,
    String gameName,
    String trackName
) {
    public static RevealResponse from(Track track) {
        return new RevealResponse(
            track.getId(),
            track.getGame().getFranchise().getName(),
            track.getGame().getName(),
            track.getName()
        );
    }
}
