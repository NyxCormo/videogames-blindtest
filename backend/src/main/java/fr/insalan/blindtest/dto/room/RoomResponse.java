package fr.insalan.blindtest.dto.room;

import java.time.Instant;
import java.util.List;

import fr.insalan.blindtest.room.Room;
import fr.insalan.blindtest.room.RoomPhase;
import fr.insalan.blindtest.room.RoomVisibility;
import fr.insalan.blindtest.room.ScoringMode;
import fr.insalan.blindtest.room.SoundMode;

public record RoomResponse(
    String code,
    Integer blindtestId,
    String blindtestName,
    long trackCount,
    Integer hostId,
    RoomVisibility visibility,
    SoundMode soundMode,
    ScoringMode scoringMode,
    int answerSeconds,
    int maxAttempts,
    Instant createdAt,
    long version,
    RoomPhase phase,
    List<RoomPlayerResponse> players,
    CurrentTrackResponse currentTrack
) {
    public static RoomResponse from(Room room) {
        synchronized (room) {
            return new RoomResponse(
                room.getCode(),
                room.getBlindtestId(),
                room.getBlindtestName(),
                room.getTrackCount(),
                room.getHostId(),
                room.getVisibility(),
                room.getSoundMode(),
                room.getScoringMode(),
                room.getAnswerSeconds(),
                room.getMaxAttempts(),
                room.getCreatedAt(),
                room.getVersion(),
                room.getPhase(),
                players(room),
                CurrentTrackResponse.from(room)
            );
        }
    }

    private static List<RoomPlayerResponse> players(Room room) {
        boolean revealed = room.getPhase() == RoomPhase.REVEAL;
        return room.players().stream()
            .map(player -> RoomPlayerResponse.from(player, room.round(player.listenerId()), revealed))
            .toList();
    }
}
