package fr.insalan.blindtest.room;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

class InfinitePickerTests {

    private static RoomTrack track(int trackId, int gameId, int franchiseId) {
        return new RoomTrack(trackId, gameId, franchiseId, "/api/tracks/" + trackId + "/audio", null);
    }

    private static InfiniteSettings settings(DrawMode drawMode, int franchiseGap) {
        return new InfiniteSettings(drawMode, franchiseGap);
    }

    @Test
    void nothingToPlayGivesNoTrack() {
        assertNull(InfinitePicker.pick(List.of(), List.of(), settings(DrawMode.TRACK, 10), new Random(1)));
    }

    @Test
    void skipsRecentTracksAndRecentFranchises() {
        List<RoomTrack> playable = List.of(track(1, 10, 100), track(2, 10, 100), track(3, 20, 200), track(4, 30, 300));
        List<RoomTrack> recent = List.of(track(3, 20, 200), track(1, 10, 100));
        Random random = new Random(1);

        for (int i = 0; i < 50; i++) {
            assertEquals(4, InfinitePicker.pick(playable, recent, settings(DrawMode.TRACK, 2), random).trackId());
        }
    }

    @Test
    void shortensTheFranchiseGapRatherThanBlocking() {
        List<RoomTrack> playable = List.of(track(1, 10, 100), track(2, 10, 100), track(3, 20, 200));
        List<RoomTrack> recent = List.of(track(1, 10, 100), track(3, 20, 200));

        RoomTrack picked = InfinitePicker.pick(playable, recent, settings(DrawMode.TRACK, 10), new Random(1));

        assertEquals(2, picked.trackId());
    }

    @Test
    void theOldestTrackComesBackWhenAllHaveBeenPlayed() {
        List<RoomTrack> playable = List.of(track(1, 10, 100), track(2, 20, 200), track(3, 30, 300));
        List<RoomTrack> recent = List.of(track(2, 20, 200), track(1, 10, 100), track(3, 30, 300));

        RoomTrack picked = InfinitePicker.pick(playable, recent, settings(DrawMode.TRACK, 0), new Random(1));

        assertEquals(2, picked.trackId());
    }

    @Test
    void theFranchiseDrawGivesEachFranchiseTheSameChance() {
        List<RoomTrack> playable = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            playable.add(track(i, 10 + i % 3, 100));
        }
        playable.add(track(99, 50, 500));
        Random random = new Random(1);

        int byTrack = 0;
        int byFranchise = 0;
        for (int i = 0; i < 2000; i++) {
            if (InfinitePicker.pick(playable, List.of(), settings(DrawMode.TRACK, 0), random).franchiseId() == 500) {
                byTrack++;
            }
            if (InfinitePicker.pick(playable, List.of(), settings(DrawMode.FRANCHISE, 0), random).franchiseId() == 500) {
                byFranchise++;
            }
        }

        assertTrue(byTrack > 120 && byTrack < 280, "musique au hasard : " + byTrack);
        assertTrue(byFranchise > 850 && byFranchise < 1150, "franchise au hasard : " + byFranchise);
        assertFalse(byTrack > byFranchise);
    }
}
