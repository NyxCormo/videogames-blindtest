package fr.insalan.blindtest.controller;

import java.util.List;
import java.util.Random;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.insalan.blindtest.dto.DiscoverTrackResponse;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.repository.TrackRepository;

@RestController
@RequestMapping("/api/discover")
public class DiscoverController {

    private final TrackRepository trackRepository;
    private final Random random = new Random();

    public DiscoverController(TrackRepository trackRepository) {
        this.trackRepository = trackRepository;
    }

    @GetMapping("/next")
    public DiscoverTrackResponse next(@RequestParam Integer listenerId) {
        List<Track> candidates = trackRepository.findPlayableUnknownByListener(listenerId);
        if (candidates.isEmpty()) {
            return new DiscoverTrackResponse(null, null, true);
        }
        Track track = candidates.get(random.nextInt(candidates.size()));
        return new DiscoverTrackResponse(track.getId(), track.getAudioLink(), false);
    }
}
