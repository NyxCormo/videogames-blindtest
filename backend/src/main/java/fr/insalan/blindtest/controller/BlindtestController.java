package fr.insalan.blindtest.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import fr.insalan.blindtest.dto.BlindtestResponse;
import fr.insalan.blindtest.dto.BlindtestSessionResponse;
import fr.insalan.blindtest.dto.CreateBlindtestRequest;
import fr.insalan.blindtest.dto.DifficultyBandRequest;
import fr.insalan.blindtest.dto.GuessFranchiseRequest;
import fr.insalan.blindtest.dto.GuessFranchiseResponse;
import fr.insalan.blindtest.dto.GuessRequest;
import fr.insalan.blindtest.dto.GuessResponse;
import fr.insalan.blindtest.dto.LeaderboardEntryResponse;
import fr.insalan.blindtest.dto.RevealResponse;
import fr.insalan.blindtest.game.BlindtestGenerator;
import fr.insalan.blindtest.game.BlindtestPlayer;
import fr.insalan.blindtest.game.DifficultyBand;
import fr.insalan.blindtest.game.GenerationStrategy;
import fr.insalan.blindtest.model.Blindtest;
import fr.insalan.blindtest.model.BlindtestScore;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.repository.BlindtestRepository;
import fr.insalan.blindtest.repository.BlindtestScoreRepository;
import fr.insalan.blindtest.repository.BlindtestTrackRepository;

@RestController 
@RequestMapping("/api/blindtests")
public class BlindtestController {
    
    private final BlindtestRepository blindtestRepository;
    private final BlindtestTrackRepository blindtestTrackRepository;
    private final BlindtestScoreRepository blindtestScoreRepository;
    private final BlindtestGenerator blindtestGenerator;
    private final BlindtestPlayer blindtestPlayer;

    public BlindtestController(
        BlindtestRepository blindtestRepository, 
        BlindtestTrackRepository blindtestTrackRepository,
        BlindtestScoreRepository blindtestScoreRepository,
        BlindtestGenerator blindtestGenerator,
        BlindtestPlayer blindtestPlayer
    ){
        this.blindtestRepository = blindtestRepository;
        this.blindtestTrackRepository = blindtestTrackRepository;
        this.blindtestScoreRepository = blindtestScoreRepository;
        this.blindtestGenerator = blindtestGenerator;
        this.blindtestPlayer = blindtestPlayer;
    }

    @GetMapping 
    public List<BlindtestResponse> list() {
        return blindtestRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(BlindtestResponse::from)
            .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BlindtestResponse create(@RequestBody CreateBlindtestRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom est obligatoire");
        }
        if (request.trackCount() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nombre de musiques doit être positif");
        }
        List<DifficultyBand> bands = validatedBands(request.difficultyBands());
        GenerationStrategy strategy = parseStrategy(request.strategy());

        Blindtest blindtest;
        try {
            List<Integer> tagIds = request.tagIds() == null ? List.of() : request.tagIds();
            boolean matchAllTags = request.matchAllTags() == null || request.matchAllTags();
            blindtest = blindtestGenerator.generate(
                request.name(),
                request.trackCount(),
                bands,
                tagIds,
                matchAllTags,
                request.maxPerGame(),
                request.maxPerFranchise(),
                strategy
            );
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        return BlindtestResponse.from(blindtest);
    }

    private List<DifficultyBand> validatedBands(List<DifficultyBandRequest> requested) {
        if (requested == null || requested.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Au moins un palier de difficulté est requis");
        }
        int totalProportion = 0;
        for (DifficultyBandRequest band : requested) {
            if (band.minDifficulty() < 0 || band.maxDifficulty() > 100 || band.minDifficulty() > band.maxDifficulty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Palier de difficulté invalide");
            }
            totalProportion += band.proportion();
        }
        if (totalProportion != 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les proportions des paliers doivent totaliser 100");
        }
        return requested.stream()
            .map(band -> new DifficultyBand(band.minDifficulty(), band.maxDifficulty(), band.proportion()))
            .toList();
    }

    private GenerationStrategy parseStrategy(String strategy) {
        if (strategy == null) {
            return null;
        }
        return switch (strategy) {
            case "random" -> GenerationStrategy.RANDOM;
            case "rareGames" -> GenerationStrategy.RARE_GAMES;
            case "rareFranchises" -> GenerationStrategy.RARE_FRANCHISES;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stratégie de génération inconnue");
        };
    }

    // Musique en cours pour ce joueur (id + lien audio seulement, jamais franchise/jeu/titre : ce serait la réponse) et son score.
    @GetMapping("/{id}/session")
    public BlindtestSessionResponse session(@PathVariable Integer id, @RequestParam Integer listenerId) {
        BlindtestScore score = blindtestPlayer.score(id, listenerId);
        Optional<Track> current = blindtestPlayer.currentTrack(id, listenerId);
        long totalTracks = blindtestTrackRepository.countByIdBlindtestId(id);

        return new BlindtestSessionResponse(
            current.map(Track::getId).orElse(null),
            current.map(Track::getAudioLink).orElse(null),
            current.isEmpty(),
            score.getTracksHeard(),
            (int) totalTracks,
            score.getGoodAnswers()
        );
    }

    @PostMapping("/{id}/guess")
    public GuessResponse guess(
        @PathVariable Integer id,
        @RequestParam Integer listenerId,
        @RequestBody GuessRequest request
    ) {
        if (request.gameId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le jeu est obligatoire");
        }
        try {
            Optional<Track> revealed = blindtestPlayer.guess(id, listenerId, request.gameId(), request.trackId());
            boolean bonusCorrect = revealed.isPresent()
                && request.trackId() != null
                && request.trackId().equals(revealed.get().getId());
            return new GuessResponse(revealed.isPresent(), bonusCorrect, revealed.map(RevealResponse::from).orElse(null));
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/{id}/guess-franchise")
    public GuessFranchiseResponse guessFranchise(
        @PathVariable Integer id,
        @RequestParam Integer listenerId,
        @RequestBody GuessFranchiseRequest request
    ) {
        if (request.franchiseId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La franchise est obligatoire");
        }
        try {
            boolean correct = blindtestPlayer.guessFranchise(id, listenerId, request.franchiseId());
            return new GuessFranchiseResponse(correct);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/{id}/pass")
    public RevealResponse pass(@PathVariable Integer id, @RequestParam Integer listenerId) {
        try {
            return RevealResponse.from(blindtestPlayer.pass(id, listenerId));
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/{id}/know-anyway")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void knowAnyway(@RequestParam Integer listenerId, @RequestParam Integer trackId) {
        blindtestPlayer.knowAnyway(listenerId, trackId);
    }

    @GetMapping("/{id}/leaderboard")
    public List<LeaderboardEntryResponse> leaderboard(@PathVariable Integer id) {
        return blindtestScoreRepository.findWithListenerByBlindtestId(id).stream()
            .map(LeaderboardEntryResponse::from)
            .toList();
    }
}
