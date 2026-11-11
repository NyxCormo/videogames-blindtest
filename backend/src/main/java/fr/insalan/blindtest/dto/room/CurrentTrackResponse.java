package fr.insalan.blindtest.dto.room;

import java.time.Instant;

import fr.insalan.blindtest.dto.blindtest.RevealResponse;
import fr.insalan.blindtest.room.Room;
import fr.insalan.blindtest.room.RoomPhase;
import fr.insalan.blindtest.room.RoomTrack;

public record CurrentTrackResponse(
    int position,
    Integer trackId,
    String audioLink,
    Instant startsAt,
    Instant endsAt,
    RevealResponse reveal
) {
    public static CurrentTrackResponse from(Room room) {
        RoomTrack track = room.currentTrack();
        if (track == null) {
            return null;
        }
        return new CurrentTrackResponse(
            room.getPosition(),
            track.trackId(),
            track.audioLink(),
            room.getStartsAt(),
            room.getEndsAt(),
            room.getPhase() == RoomPhase.REVEAL ? track.answer() : null
        );
    }
}
