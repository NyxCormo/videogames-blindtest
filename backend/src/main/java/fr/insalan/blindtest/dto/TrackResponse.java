package fr.insalan.blindtest.dto;

import fr.insalan.blindtest.model.Track;

public record TrackResponse (
    Integer id,
    String name,
    String gameName,
    String franchiseName
)
{
    public static TrackResponse from(Track track) {
        return new TrackResponse(
            track.getId(),
            track.getName(),
            track.getGame().getName(),
            track.getGame().getFranchise().getName()
        );
    }
    
}
