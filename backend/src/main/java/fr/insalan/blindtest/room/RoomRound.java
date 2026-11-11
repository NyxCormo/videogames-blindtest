package fr.insalan.blindtest.room;

public record RoomRound(
    int position,
    Integer trackId,
    int score,
    int roundPoints,
    int attemptsRemaining,
    boolean franchiseFound,
    boolean gameFound,
    boolean bonusFound,
    boolean done
) {
}
