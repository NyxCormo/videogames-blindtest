package fr.insalan.blindtest.room;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Room {

    private final String code;
    private final Integer blindtestId;
    private final String blindtestName;
    private final long trackCount;
    private final Integer hostId;
    private final RoomVisibility visibility;
    private final SoundMode soundMode;
    private final ScoringMode scoringMode;
    private final int answerSeconds;
    private final Instant createdAt = Instant.now();
    private final Map<Integer, RoomPlayer> players = new LinkedHashMap<>();

    public Room(
        String code,
        Integer blindtestId,
        String blindtestName,
        long trackCount,
        RoomPlayer host,
        RoomVisibility visibility,
        SoundMode soundMode,
        ScoringMode scoringMode,
        int answerSeconds
    ) {
        this.code = code;
        this.blindtestId = blindtestId;
        this.blindtestName = blindtestName;
        this.trackCount = trackCount;
        this.hostId = host.listenerId();
        this.visibility = visibility;
        this.soundMode = soundMode;
        this.scoringMode = scoringMode;
        this.answerSeconds = answerSeconds;
        players.put(host.listenerId(), host);
    }

    public synchronized void join(RoomPlayer player) {
        players.putIfAbsent(player.listenerId(), player);
    }

    public synchronized List<RoomPlayer> players() {
        return new ArrayList<>(players.values());
    }

    public String getCode() {
        return code;
    }

    public Integer getBlindtestId() {
        return blindtestId;
    }

    public String getBlindtestName() {
        return blindtestName;
    }

    public long getTrackCount() {
        return trackCount;
    }

    public Integer getHostId() {
        return hostId;
    }

    public RoomVisibility getVisibility() {
        return visibility;
    }

    public SoundMode getSoundMode() {
        return soundMode;
    }

    public ScoringMode getScoringMode() {
        return scoringMode;
    }

    public int getAnswerSeconds() {
        return answerSeconds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
