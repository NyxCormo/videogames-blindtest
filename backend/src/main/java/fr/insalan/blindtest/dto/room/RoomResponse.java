package fr.insalan.blindtest.dto.room;

import java.time.Instant;
import java.util.List;

import fr.insalan.blindtest.dto.blindtest.RevealResponse;
import fr.insalan.blindtest.room.DrawMode;
import fr.insalan.blindtest.room.Room;
import fr.insalan.blindtest.room.RoomMode;
import fr.insalan.blindtest.room.RoomPhase;
import fr.insalan.blindtest.room.RoomTrack;
import fr.insalan.blindtest.room.RoomVisibility;
import fr.insalan.blindtest.room.ScoringMode;

public record RoomResponse(
    String code,
    Integer blindtestId,
    String blindtestName,
    long trackCount,
    Integer hostId,
    String hostName,
    RoomVisibility visibility,
    RoomMode mode,
    boolean playersCanSeek,
    ScoringMode scoringMode,
    int answerSeconds,
    int maxAttempts,
    boolean infinite,
    DrawMode drawMode,
    Integer franchiseGap,
    Instant createdAt,
    long version,
    RoomPhase phase,
    List<RoomPlayerResponse> players,
    CurrentTrackResponse currentTrack,
    List<RevealResponse> playedTracks
) {
    public static RoomResponse from(Room room) {
        synchronized (room) {
            return new RoomResponse(
                room.getCode(),
                room.getBlindtestId(),
                room.getBlindtestName(),
                room.getTrackCount(),
                room.getHostId(),
                room.getHostName(),
                room.getVisibility(),
                room.getMode(),
                room.isPlayersCanSeek(),
                room.getScoringMode(),
                room.getAnswerSeconds(),
                room.getMaxAttempts(),
                room.isInfinite(),
                room.isInfinite() ? room.getInfinite().drawMode() : null,
                room.isInfinite() ? room.getInfinite().franchiseGap() : null,
                room.getCreatedAt(),
                room.getVersion(),
                room.getPhase(),
                players(room),
                CurrentTrackResponse.from(room),
                playedTracks(room)
            );
        }
    }

    private static List<RoomPlayerResponse> players(Room room) {
        boolean revealed = room.getPhase() == RoomPhase.REVEAL;
        return room.players().stream()
            .map(player -> RoomPlayerResponse.from(
                player,
                room.round(player.listenerId()),
                room.totals(player.listenerId()),
                room.isPresent(player.listenerId()),
                revealed))
            .toList();
    }

    private static List<RevealResponse> playedTracks(Room room) {
        if (room.getPhase() != RoomPhase.FINISHED) {
            return List.of();
        }
        return room.revealedTracks().stream()
            .map(RoomTrack::answer)
            .toList();
    }
}
