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
            RoomVisibility.PUBLIC, RoomMode.MULTI, true, scoringMode, 30, 2);
        room.join(new RoomPlayer(NYX, "Nyx"), NOW);
        room.join(new RoomPlayer(RAVEN, "Raven"), NOW);
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
        room.join(new RoomPlayer(RAVEN, "Raven"), NOW);
        assertEquals(joined, room.getVersion());

        room.start(tracks, NOW);
        room.pass(NYX, MUSIC_STARTS);
        room.reveal(0);
        assertEquals(joined + 3, room.getVersion());
    }

    @Test
    void stoppingKeepsTheRevealedPointsOnly() {
        Room room = room(ScoringMode.FIRST_COME);
        room.start(tracks, NOW);
        room.guess(NYX, DAWN_GAME, null, MUSIC_STARTS);
        room.reveal(0);
        room.next(0, NOW.plusSeconds(40));
        room.guess(NYX, 101, null, NOW.plusSeconds(45));

        assertTrue(room.stop());
        assertEquals(RoomPhase.FINISHED, room.getPhase());
        assertNull(room.currentTrack());
        assertEquals(1000, room.round(NYX).score());
        assertFalse(room.reveal(1));
        assertFalse(room.next(1, NOW.plusSeconds(60)));
        assertFalse(room.stop());
    }

    @Test
    void aWaitingRoomCanBeStopped() {
        Room room = room(ScoringMode.SPEED);

        assertTrue(room.stop());
        assertEquals(RoomPhase.FINISHED, room.getPhase());
        assertThrows(IllegalStateException.class, () -> room.start(tracks, NOW));
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
        room.join(new RoomPlayer(3, "Lily"), NOW);
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
        assertEquals(DAWN_FRANCHISE, franchise.foundFranchiseId());
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
    void totalsAndPlayedTracksCountOnlyRevealedTracks() {
        Room room = room(ScoringMode.SPEED);
        room.start(tracks, NOW);
        room.guess(NYX, DAWN_GAME, 10, MUSIC_STARTS);
        room.guessFranchise(RAVEN, DAWN_FRANCHISE, MUSIC_STARTS);
        assertEquals(new RoomTotals(0, 0, 0, 0), room.totals(NYX));
        assertEquals(List.of(), room.revealedTracks());

        room.reveal(0);
        room.next(0, NOW.plusSeconds(40));
        room.guess(NYX, 101, null, NOW.plusSeconds(45));
        room.stop();

        assertEquals(new RoomTotals(1, 0, 1, 1), room.totals(NYX));
        assertEquals(new RoomTotals(0, 1, 0, 1), room.totals(RAVEN));
        assertEquals(List.of(tracks.get(0)), room.revealedTracks());
    }

    @Test
    void aPlayerIsAbsentAfterTheTimeoutOrWhenLeaving() {
        Room room = room(ScoringMode.SPEED);
        assertFalse(room.seen(NYX, NOW.plusSeconds(20)));

        assertFalse(room.checkPresence(NOW.plusSeconds(24)));
        assertTrue(room.checkPresence(NOW.plusSeconds(25)));
        assertTrue(room.isPresent(NYX));
        assertFalse(room.isPresent(RAVEN));

        assertTrue(room.leave(NYX));
        assertFalse(room.leave(NYX));
        assertFalse(room.isPresent(NYX));
        assertTrue(room.seen(NYX, NOW.plusSeconds(30)));
        assertFalse(room.seen(NYX, NOW.plusSeconds(31)));
        assertFalse(room.seen(99, NOW));
    }

    @Test
    void onlyPresentPlayersAreWaitedFor() {
        Room room = room(ScoringMode.SPEED);
        room.start(tracks, NOW);
        room.pass(NYX, MUSIC_STARTS);
        assertFalse(room.everyoneDone());

        room.leave(RAVEN);
        assertTrue(room.everyoneDone());

        room.leave(NYX);
        assertFalse(room.everyoneDone());
    }

    @Test
    void aTrackIsHeardWhenPresentOrAfterAnswering() {
        Room room = room(ScoringMode.SPEED);
        room.join(new RoomPlayer(3, "Lily"), NOW);
        room.start(tracks, NOW);
        room.pass(RAVEN, MUSIC_STARTS);
        room.leave(RAVEN);
        room.leave(3);
        room.reveal(0);

        assertEquals(1, room.totals(NYX).tracksHeard());
        assertEquals(1, room.totals(RAVEN).tracksHeard());
        assertEquals(0, room.totals(3).tracksHeard());
    }

    @Test
    void answersCloseWhenTheTimeIsUp() {
        Room room = room(ScoringMode.SPEED);
        room.start(tracks, NOW);

        assertEquals(500, room.guess(NYX, DAWN_GAME, null, MUSIC_STARTS.plusSeconds(30).minusMillis(1)).roundPoints());
        assertThrows(IllegalStateException.class, () -> room.pass(RAVEN, MUSIC_STARTS.plusSeconds(30)));
    }

    @Test
    void thePresenterDoesNotPlay() {
        Room room = new Room("ABCD", 1, "Stellar Blade", 2, new RoomPlayer(NYX, "Nyx"),
            RoomVisibility.PUBLIC, RoomMode.PRESENTATION, true, ScoringMode.SPEED, 30, 2);

        assertFalse(room.join(new RoomPlayer(NYX, "Nyx"), NOW));
        assertTrue(room.join(new RoomPlayer(RAVEN, "Raven"), NOW));
        assertFalse(room.hasPlayer(NYX));
        assertEquals(NYX, room.getHostId());
        assertEquals("Nyx", room.getHostName());
        assertEquals(1, room.players().size());
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
