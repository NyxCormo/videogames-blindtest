package fr.insalan.blindtest.dto;

import fr.insalan.blindtest.model.Track;

public record TrackResponse (
    Integer id,
    String name,
    Integer gameId,
    String gameName,
    String franchiseName,
    String khinsiderLink,
    String youtubeLink,
    String audioLink
)
{
    public static TrackResponse from(Track track) {
        return new TrackResponse(
            track.getId(),
            track.getName(),
            track.getGame().getId(),
            track.getGame().getName(),
            track.getGame().getFranchise().getName(),
            track.getKhinsiderLink(),
            track.getYoutubeLink(),
            track.getAudioLink()
        );
    }

}
