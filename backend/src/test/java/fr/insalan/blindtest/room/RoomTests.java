package fr.insalan.blindtest.room;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class RoomTests {

    private static final Instant NOW = Instant.parse("2026-11-10T20:00:00Z");
    private static final Instant MUSIC_STARTS = NOW.plus(Room.LEAD_TIME);
    private static final int NYX = 1;
    private static final int RAVEN = 2;
    private static final int DAWN_GAME = 100;
    private static final int DAWN_FRANCHISE = 1000;

    private final List<RoomTrack> tracks = List.of(
        new RoomTrack(10, DAWN_GAME, DAWN_FRANCHISE, "/api/tracks/10/audio", null),
        new RoomTrack(11, 101, 1001, "/api/tracks/11/audio", null)
    );

    private Room room(ScoringMode scoringMode) {
        Room room = new Room("ABCD", 1, "Stellar Blade", 2, new RoomPlayer(NYX, "Nyx"),
            RoomVisibility.PUBLIC, SoundMode.EVERY_DEVICE, scoringMode, 30, 2);
        room.join(new RoomPlayer(RAVEN, "Raven"));
        return room;
    }

    @Test
    void startsTheFirstTrackAfterTheLeadTime() {
        Room room = room(ScoringMode.SPEED);
        room.start(tracks, NOW);

        assertEquals(RoomPhase.PLAYING, room.getPhase());
        assertEquals(0, room.getPosition());
        assertEquals(10, room.currentTrack().trackId());
        assertEquals(MUSIC_STARTS, room.getStartsAt());
        assertEquals(MUSIC_STARTS.plusSeconds(30), room.getEndsAt());
    }

    @Test
    void cannotStartTwice() {
        Room room = room(ScoringMode.SPEED);
        room.start(tracks, NOW);

        assertThrows(IllegalStateException.class, () -> room.start(tracks, NOW));
    }

    @Test
    void revealsEachTrackOnceThenFinishes() {
        Room room = room(ScoringMode.SPEED);
        room.start(tracks, NOW);

        assertFalse(room.next(0, NOW));
        assertFalse(room.reveal(1));
        assertTrue(room.reveal(0));
        assertFalse(room.reveal(0));
        assertEquals(RoomPhase.REVEAL, room.getPhase());

        Instant later = NOW.plusSeconds(40);
        assertTrue(room.next(0, later));
        assertEquals(RoomPhase.PLAYING, room.getPhase());
        assertEquals(1, room.getPosition());
        assertEquals(later.plus(Room.LEAD_TIME), room.getStartsAt());

        assertTrue(room.reveal(1));
        assertTrue(room.next(1, later));
        assertEquals(RoomPhase.FINISHED, room.getPhase());
        assertNull(room.currentTrack());
    }

    @Test
    void theVersionGrowsWithEachChange() {
        Room room = room(ScoringMode.SPEED);
        long joined = room.getVersion();
        room.join(new RoomPlayer(RAVEN, "Raven"));
        assertEquals(joined, room.getVersion());

        room.start(tracks, NOW);
        room.pass(NYX, MUSIC_STARTS);
        room.reveal(0);
        assertEquals(joined + 3, room.getVersion());
    }

    @Test
    void speedPointsGoFrom1000To500WithTheTimeLeft() {
        Room room = room(ScoringMode.SPEED);
        room.start(tracks, NOW);

        assertEquals(1000, room.guess(NYX, DAWN_GAME, null, MUSIC_STARTS).roundPoints());
        assertEquals(750, room.guess(RAVEN, DAWN_GAME, null, MUSIC_STARTS.plusSeconds(15)).roundPoints());
    }

    @Test
    void firstComePointsDependOnTheOrder() {
        Room room = room(ScoringMode.FIRST_COME);
        room.join(new RoomPlayer(3, "Lily"));
        room.start(tracks, NOW);

        assertEquals(1000, room.guess(RAVEN, DAWN_GAME, null, MUSIC_STARTS.plusSeconds(20)).roundPoints());
        assertEquals(800, room.guess(NYX, DAWN_GAME, null, MUSIC_STARTS.plusSeconds(21)).roundPoints());
        assertEquals(600, room.guess(3, DAWN_GAME, null, MUSIC_STARTS.plusSeconds(22)).roundPoints());
    }

    @Test
    void theFranchiseThenTheGameAndTheTrack() {
        Room room = room(ScoringMode.FIRST_COME);
        room.start(tracks, NOW);

        RoomRound franchise = room.guessFranchise(NYX, DAWN_FRANCHISE, MUSIC_STARTS);
        assertTrue(franchise.franchiseFound());
        assertEquals(250, franchise.roundPoints());
        assertEquals(1, franchise.attemptsRemaining());
        assertFalse(franchise.done());

        RoomRound game = room.guess(NYX, DAWN_GAME, 10, MUSIC_STARTS);
        assertTrue(game.gameFound());
        assertTrue(game.bonusFound());
        assertEquals(1250, game.roundPoints());
        assertTrue(game.done());
    }

    @Test
    void wrongAnswersUseTheAttempts() {
        Room room = room(ScoringMode.SPEED);
        room.start(tracks, NOW);

        room.guess(NYX, 999, null, MUSIC_STARTS);
        RoomRound last = room.guessFranchise(NYX, 999, MUSIC_STARTS);

        assertTrue(last.done());
        assertFalse(last.gameFound());
        assertEquals(0, last.roundPoints());
        assertThrows(IllegalStateException.class, () -> room.guess(NYX, DAWN_GAME, null, MUSIC_STARTS));
    }

    @Test
    void answersOnlyWhileTheMusicPlays() {
        Room room = room(ScoringMode.SPEED);
        assertThrows(IllegalStateException.class, () -> room.pass(NYX, NOW));

        room.start(tracks, NOW);
        assertThrows(IllegalStateException.class, () -> room.pass(NYX, MUSIC_STARTS.minusMillis(1)));

        room.reveal(0);
        assertThrows(IllegalStateException.class, () -> room.pass(NYX, MUSIC_STARTS));
    }

    @Test
    void pointsCountAtTheRevealThenANewRoundStarts() {
        Room room = room(ScoringMode.FIRST_COME);
        room.start(tracks, NOW);

        room.guess(NYX, DAWN_GAME, null, MUSIC_STARTS);
        assertFalse(room.everyoneDone());
        assertEquals(0, room.round(NYX).score());
        room.pass(RAVEN, MUSIC_STARTS);
        assertTrue(room.everyoneDone());

        room.reveal(0);
        assertEquals(1000, room.round(NYX).score());
        assertEquals(0, room.round(RAVEN).score());

        room.next(0, NOW.plusSeconds(40));
        RoomRound next = room.round(NYX);
        assertEquals(1000, next.score());
        assertEquals(0, next.roundPoints());
        assertEquals(2, next.attemptsRemaining());
        assertFalse(next.done());
        assertFalse(room.everyoneDone());
    }
}
