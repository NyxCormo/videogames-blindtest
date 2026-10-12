package fr.insalan.blindtest.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import fr.insalan.blindtest.dto.ListenerStatsResponse;
import fr.insalan.blindtest.dto.StatsResponse;
import fr.insalan.blindtest.repository.FranchiseRepository;
import fr.insalan.blindtest.repository.GameRepository;
import fr.insalan.blindtest.repository.KnowledgeRepository;
import fr.insalan.blindtest.repository.ListenerRepository;
import fr.insalan.blindtest.repository.TrackRepository;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final FranchiseRepository franchiseRepository;
    private final GameRepository gameRepository;
    private final TrackRepository trackRepository;
    private final ListenerRepository listenerRepository;
    private final KnowledgeRepository knowledgeRepository;

    public StatsController(
        FranchiseRepository franchiseRepository,
        GameRepository gameRepository,
        TrackRepository trackRepository,
        ListenerRepository listenerRepository,
        KnowledgeRepository knowledgeRepository
    ) {
        this.franchiseRepository = franchiseRepository;
        this.gameRepository = gameRepository;
        this.trackRepository = trackRepository;
        this.listenerRepository = listenerRepository;
        this.knowledgeRepository = knowledgeRepository;
    }

    @GetMapping
    public StatsResponse stats() {
        return new StatsResponse(
            trackRepository.count(),
            trackRepository.countByAudioLinkIsNotNull(),
            gameRepository.count(),
            franchiseRepository.count(),
            listenerRepository.count(),
            knowledgeRepository.count()
        );
    }

    @GetMapping("/listeners/{listenerId}")
    public ListenerStatsResponse listenerStats(@PathVariable Integer listenerId) {
        if (!listenerRepository.existsById(listenerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pseudo introuvable");
        }
        return new ListenerStatsResponse(
            knowledgeRepository.countByListenerId(listenerId),
            knowledgeRepository.countKnownByListenerId(listenerId),
            trackRepository.countPlayableUnknownByListener(listenerId)
        );
    }
}
