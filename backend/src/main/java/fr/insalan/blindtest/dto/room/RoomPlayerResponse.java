package fr.insalan.blindtest.dto.room;

import fr.insalan.blindtest.room.RoomPlayer;
import fr.insalan.blindtest.room.RoomRound;

public record RoomPlayerResponse(
    Integer listenerId,
    String name,
    int score,
    boolean done,
    Integer roundPoints
) {
    public static RoomPlayerResponse from(RoomPlayer player, RoomRound round, boolean revealed) {
        return new RoomPlayerResponse(
            player.listenerId(),
            player.name(),
            round.score(),
            round.done(),
            revealed ? round.roundPoints() : null
        );
    }
}
