package fr.insalan.blindtest.room;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Room {

    public static final Duration LEAD_TIME = Duration.ofSeconds(4);

    private static final int[] FIRST_COME_POINTS = {1000, 800, 600, 400, 200};
    private static final int SPEED_MAX_POINTS = 1000;
    private static final int SPEED_MIN_POINTS = 500;

    private final String code;
    private final Integer blindtestId;
    private final String blindtestName;
    private final long trackCount;
    private final Integer hostId;
    private final RoomVisibility visibility;
    private final SoundMode soundMode;
    private final ScoringMode scoringMode;
    private final int answerSeconds;
    private final int maxAttempts;
    private final Instant createdAt = Instant.now();
    private final Map<Integer, RoomPlayer> players = new LinkedHashMap<>();
    private final Map<Integer, RoomScore> scores = new LinkedHashMap<>();
    private long version;
    private RoomPhase phase = RoomPhase.WAITING;
    private List<RoomTrack> tracks = List.of();
    private int position = -1;
    private Instant startsAt;
    private Instant endsAt;
    private int gameFinders;

    public Room(
        String code,
        Integer blindtestId,
        String blindtestName,
        long trackCount,
        RoomPlayer host,
        RoomVisibility visibility,
        SoundMode soundMode,
        ScoringMode scoringMode,
        int answerSeconds,
        int maxAttempts
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
        this.maxAttempts = maxAttempts;
        players.put(host.listenerId(), host);
        scores.put(host.listenerId(), new RoomScore());
    }

    public synchronized boolean join(RoomPlayer player) {
        if (players.putIfAbsent(player.listenerId(), player) != null) {
            return false;
        }
        scores.put(player.listenerId(), new RoomScore());
        version++;
        return true;
    }

    public synchronized boolean hasPlayer(Integer listenerId) {
        return players.containsKey(listenerId);
    }

    public synchronized void start(List<RoomTrack> tracks, Instant now) {
        if (phase != RoomPhase.WAITING) {
            throw new IllegalStateException("La partie a déjà commencé");
        }
        this.tracks = List.copyOf(tracks);
        play(0, now);
    }

    public synchronized RoomRound guess(Integer listenerId, Integer gameId, Integer trackId, Instant now) {
        RoomScore score = scoreToAnswer(listenerId, now);
        RoomTrack track = tracks.get(position);
        if (track.gameId().equals(gameId)) {
            score.findGame(gamePoints(now), track.trackId().equals(trackId));
            gameFinders++;
        } else {
            score.missGame();
        }
        return round(listenerId);
    }

    public synchronized RoomRound guessFranchise(Integer listenerId, Integer franchiseId, Instant now) {
        RoomScore score = scoreToAnswer(listenerId, now);
        score.guessFranchise(tracks.get(position).franchiseId().equals(franchiseId));
        return round(listenerId);
    }

    public synchronized RoomRound pass(Integer listenerId, Instant now) {
        scoreToAnswer(listenerId, now).pass();
        return round(listenerId);
    }

    public synchronized RoomRound round(Integer listenerId) {
        RoomTrack track = currentTrack();
        return scores.get(listenerId).round(position, track == null ? null : track.trackId(), maxAttempts);
    }

    public synchronized boolean everyoneDone() {
        return phase == RoomPhase.PLAYING && scores.values().stream().allMatch(score -> score.done(maxAttempts));
    }

    public synchronized boolean reveal(int position) {
        if (phase != RoomPhase.PLAYING || this.position != position) {
            return false;
        }
        phase = RoomPhase.REVEAL;
        scores.values().forEach(RoomScore::endRound);
        version++;
        return true;
    }

    public synchronized boolean next(int position, Instant now) {
        if (phase != RoomPhase.REVEAL || this.position != position) {
            return false;
        }
        if (position + 1 < tracks.size()) {
            play(position + 1, now);
        } else {
            phase = RoomPhase.FINISHED;
            version++;
        }
        return true;
    }

    public synchronized boolean stop() {
        if (phase == RoomPhase.FINISHED) {
            return false;
        }
        phase = RoomPhase.FINISHED;
        version++;
        return true;
    }

    private void play(int position, Instant now) {
        phase = RoomPhase.PLAYING;
        this.position = position;
        startsAt = now.plus(LEAD_TIME);
        endsAt = startsAt.plusSeconds(answerSeconds);
        gameFinders = 0;
        scores.values().forEach(RoomScore::newRound);
        version++;
    }

    private RoomScore scoreToAnswer(Integer listenerId, Instant now) {
        if (phase != RoomPhase.PLAYING) {
            throw new IllegalStateException("Aucune musique en cours");
        }
        if (now.isBefore(startsAt)) {
            throw new IllegalStateException("La musique n'a pas encore commencé");
        }
        RoomScore score = scores.get(listenerId);
        if (score.done(maxAttempts)) {
            throw new IllegalStateException("Plus de réponse possible sur cette musique");
        }
        version++;
        return score;
    }

    private int gamePoints(Instant now) {
        if (scoringMode == ScoringMode.FIRST_COME) {
            return FIRST_COME_POINTS[Math.min(gameFinders, FIRST_COME_POINTS.length - 1)];
        }
        long total = Duration.ofSeconds(answerSeconds).toMillis();
        long left = Math.clamp(Duration.between(now, endsAt).toMillis(), 0, total);
        return SPEED_MIN_POINTS + (int) ((SPEED_MAX_POINTS - SPEED_MIN_POINTS) * left / total);
    }

    public synchronized RoomTrack currentTrack() {
        if (phase == RoomPhase.WAITING || phase == RoomPhase.FINISHED) {
            return null;
        }
        return tracks.get(position);
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

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public synchronized long getVersion() {
        return version;
    }

    public synchronized RoomPhase getPhase() {
        return phase;
    }

    public synchronized int getPosition() {
        return position;
    }

    public synchronized Instant getStartsAt() {
        return startsAt;
    }

    public synchronized Instant getEndsAt() {
        return endsAt;
    }
}
