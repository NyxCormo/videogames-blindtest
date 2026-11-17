package fr.insalan.blindtest.room;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import fr.insalan.blindtest.repository.TrackRepository;

@Component
public class InfinitePicker {

    static final int TRACK_MEMORY = 100;

    private final TrackRepository trackRepository;
    private final Random random = new Random();

    public InfinitePicker(TrackRepository trackRepository) {
        this.trackRepository = trackRepository;
    }

    public RoomTrack pick(Room room) {
        List<RoomTrack> playable = trackRepository.findWithGameByAudioLinkIsNotNull().stream()
            .map(RoomTrack::from)
            .toList();
        return pick(playable, room.recentTracks(TRACK_MEMORY), room.getInfinite(), random);
    }

    static RoomTrack pick(List<RoomTrack> playable, List<RoomTrack> recent, InfiniteSettings settings, Random random) {
        if (playable.isEmpty()) {
            return null;
        }
        int memory = Math.min(recent.size(), playable.size() - 1);
        Set<Integer> recentTracks = last(recent, memory).stream()
            .map(RoomTrack::trackId)
            .collect(Collectors.toSet());
        List<RoomTrack> fresh = playable.stream()
            .filter(track -> !recentTracks.contains(track.trackId()))
            .toList();
        for (int gap = Math.min(settings.franchiseGap(), recent.size()); gap > 0; gap--) {
            Set<Integer> recentFranchises = last(recent, gap).stream()
                .map(RoomTrack::franchiseId)
                .collect(Collectors.toSet());
            List<RoomTrack> candidates = fresh.stream()
                .filter(track -> !recentFranchises.contains(track.franchiseId()))
                .toList();
            if (!candidates.isEmpty()) {
                return draw(candidates, settings.drawMode(), random);
            }
        }
        return draw(fresh, settings.drawMode(), random);
    }

    private static RoomTrack draw(List<RoomTrack> candidates, DrawMode drawMode, Random random) {
        List<RoomTrack> pool = candidates;
        if (drawMode == DrawMode.FRANCHISE) {
            pool = randomGroup(pool, RoomTrack::franchiseId, random);
            pool = randomGroup(pool, RoomTrack::gameId, random);
        }
        return pool.get(random.nextInt(pool.size()));
    }

    private static List<RoomTrack> randomGroup(
        List<RoomTrack> tracks,
        Function<RoomTrack, Integer> key,
        Random random
    ) {
        Map<Integer, List<RoomTrack>> groups = tracks.stream().collect(Collectors.groupingBy(key));
        List<List<RoomTrack>> values = new ArrayList<>(groups.values());
        return values.get(random.nextInt(values.size()));
    }

    private static List<RoomTrack> last(List<RoomTrack> tracks, int count) {
        return tracks.subList(tracks.size() - count, tracks.size());
    }
}
