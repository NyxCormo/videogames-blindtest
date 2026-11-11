package fr.insalan.blindtest.room;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class RoomService {

    private static final String CODE_LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 4;

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public Room create(
        Integer blindtestId,
        String blindtestName,
        long trackCount,
        RoomPlayer host,
        RoomVisibility visibility,
        RoomMode mode,
        boolean playersCanSeek,
        ScoringMode scoringMode,
        int answerSeconds,
        int maxAttempts
    ) {
        while (true) {
            String code = newCode();
            Room room = new Room(code, blindtestId, blindtestName, trackCount, host, visibility, mode, playersCanSeek, scoringMode, answerSeconds, maxAttempts);
            if (rooms.putIfAbsent(code, room) == null) {
                return room;
            }
        }
    }

    public Optional<Room> find(String code) {
        return Optional.ofNullable(rooms.get(code.toUpperCase()));
    }

    public List<Room> publicRooms() {
        return rooms.values().stream()
            .filter(room -> room.getVisibility() == RoomVisibility.PUBLIC)
            .filter(room -> room.getPhase() != RoomPhase.FINISHED)
            .sorted(Comparator.comparing(Room::getCreatedAt).reversed())
            .toList();
    }

    public void remove(String code) {
        rooms.remove(code);
    }

    private String newCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CODE_LETTERS.charAt(random.nextInt(CODE_LETTERS.length())));
        }
        return code.toString();
    }
}
