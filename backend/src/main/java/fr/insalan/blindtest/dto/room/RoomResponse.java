package fr.insalan.blindtest.dto.room;

import java.time.Instant;
import java.util.List;

import fr.insalan.blindtest.room.Room;
import fr.insalan.blindtest.room.RoomPlayer;
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
    Instant createdAt,
    List<RoomPlayer> players
) {
    public static RoomResponse from(Room room) {
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
            room.getCreatedAt(),
            room.players()
        );
    }
}
