package fr.insalan.blindtest.controller;

import java.util.List;
import java.util.Random;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import fr.insalan.blindtest.dto.DiscoverTrackResponse;
import fr.insalan.blindtest.dto.KnowledgeEntryResponse;
import fr.insalan.blindtest.model.Knowledge;
import fr.insalan.blindtest.model.Listener;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.repository.KnowledgeRepository;
import fr.insalan.blindtest.repository.ListenerRepository;
import fr.insalan.blindtest.repository.TrackRepository;

@RestController
@RequestMapping("/api/discover")
public class DiscoverController {

    private final TrackRepository trackRepository;
    private final ListenerRepository listenerRepository;
    private final KnowledgeRepository knowledgeRepository;
    private final Random random = new Random();

    public DiscoverController(
        TrackRepository trackRepository,
        ListenerRepository listenerRepository,
        KnowledgeRepository knowledgeRepository
    ) {
        this.trackRepository = trackRepository;
        this.listenerRepository = listenerRepository;
        this.knowledgeRepository = knowledgeRepository;
    }

    @GetMapping("/next")
    public DiscoverTrackResponse next(@RequestParam Integer listenerId) {
        List<Track> candidates = trackRepository.findPlayableUnknownByListener(listenerId);
        if (candidates.isEmpty()) {
            return DiscoverTrackResponse.allDiscovered();
        }
        Track track = candidates.get(random.nextInt(candidates.size()));
        return DiscoverTrackResponse.from(track);
    }

    @GetMapping("/knowledge")
    public List<KnowledgeEntryResponse> knowledge(@RequestParam Integer listenerId) {
        return knowledgeRepository.findByListenerId(listenerId).stream()
            .map(k -> new KnowledgeEntryResponse(k.getTrack().getId(), k.isKnows()))
            .toList();
    }

    @PostMapping("/knowledge")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setKnowledge(@RequestParam Integer listenerId, @RequestParam Integer trackId, @RequestParam boolean knows) {
        Listener listener = listenerRepository.findById(listenerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Joueur inconnu"));
        Track track = trackRepository.findById(trackId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Musique inconnue"));
        knowledgeRepository.save(new Knowledge(listener, track, knows));
    }
}
