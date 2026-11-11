package fr.insalan.blindtest.dto.room;

import fr.insalan.blindtest.room.RoomMode;
import fr.insalan.blindtest.room.RoomVisibility;
import fr.insalan.blindtest.room.ScoringMode;

public record CreateRoomRequest(
    Integer blindtestId,
    RoomVisibility visibility,
    RoomMode mode,
    Boolean playersCanSeek,
    ScoringMode scoringMode,
    Integer answerSeconds
) {
}
