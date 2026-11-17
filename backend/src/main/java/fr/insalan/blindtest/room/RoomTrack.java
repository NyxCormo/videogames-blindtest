package fr.insalan.blindtest.room;

import fr.insalan.blindtest.dto.blindtest.RevealResponse;
import fr.insalan.blindtest.model.Track;

public record RoomTrack(Integer trackId, Integer gameId, Integer franchiseId, String audioLink, RevealResponse answer) {

    public static RoomTrack from(Track track) {
        return new RoomTrack(
            track.getId(),
            track.getGame().getId(),
            track.getGame().getFranchise().getId(),
            track.getAudioLink(),
            RevealResponse.from(track)
        );
    }
}
