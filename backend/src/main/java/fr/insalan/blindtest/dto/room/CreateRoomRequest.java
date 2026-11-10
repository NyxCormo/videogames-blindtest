package fr.insalan.blindtest.dto.room;

import fr.insalan.blindtest.room.RoomVisibility;
import fr.insalan.blindtest.room.ScoringMode;
import fr.insalan.blindtest.room.SoundMode;

public record CreateRoomRequest(
    Integer blindtestId,
    RoomVisibility visibility,
    SoundMode soundMode,
    ScoringMode scoringMode,
    Integer answerSeconds
) {
}
