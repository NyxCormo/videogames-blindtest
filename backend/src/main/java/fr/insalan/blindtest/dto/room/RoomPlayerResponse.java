package fr.insalan.blindtest.dto.room;

import fr.insalan.blindtest.room.RoomPlayer;
import fr.insalan.blindtest.room.RoomRound;
import fr.insalan.blindtest.room.RoomTotals;

public record RoomPlayerResponse(
    Integer listenerId,
    String name,
    int score,
    boolean done,
    Integer roundPoints,
    boolean present,
    int gamesFound,
    int franchisesFound,
    int bonusesFound,
    int tracksHeard
) {
    public static RoomPlayerResponse from(RoomPlayer player, RoomRound round, RoomTotals totals, boolean present, boolean revealed) {
        return new RoomPlayerResponse(
            player.listenerId(),
            player.name(),
            round.score(),
            round.done(),
            revealed ? round.roundPoints() : null,
            present,
            totals.gamesFound(),
            totals.franchisesFound(),
            totals.bonusesFound(),
            totals.tracksHeard()
        );
    }
}
