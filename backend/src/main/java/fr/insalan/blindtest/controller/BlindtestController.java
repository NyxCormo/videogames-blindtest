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
import fr.insalan.blindtest.dto.GuessRequest;
import fr.insalan.blindtest.dto.GuessResponse;
import fr.insalan.blindtest.dto.RevealResponse;
import fr.insalan.blindtest.game.BlindtestGenerator;
import fr.insalan.blindtest.game.BlindtestPlayer;
import fr.insalan.blindtest.model.Blindtest;
import fr.insalan.blindtest.model.BlindtestScore;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.repository.BlindtestRepository;
import fr.insalan.blindtest.repository.BlindtestTrackRepository;

@RestController 
@RequestMapping("/api/blindtests")
public class BlindtestController {
    
    private final BlindtestRepository blindtestRepository;
    private final BlindtestTrackRepository blindtestTrackRepository;
    private final BlindtestGenerator blindtestGenerator;
    private final BlindtestPlayer blindtestPlayer;

    public BlindtestController(
        BlindtestRepository blindtestRepository, 
        BlindtestTrackRepository blindtestTrackRepository,
        BlindtestGenerator blindtestGenerator,
        BlindtestPlayer blindtestPlayer
    ){
        this.blindtestRepository = blindtestRepository;
        this.blindtestTrackRepository = blindtestTrackRepository;
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
        if (request.difficulty() < 0 || request.difficulty() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La difficulté doit être comprise entre 0 et 100");
        }

        Blindtest blindtest;
        try {
            blindtest = blindtestGenerator.generate(request.name(), request.trackCount(), request.difficulty());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        return BlindtestResponse.from(blindtest);
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
        try {
            Optional<Track> revealed = blindtestPlayer.guess(id, listenerId, request.guess());
            return new GuessResponse(revealed.isPresent(), revealed.map(RevealResponse::from).orElse(null));
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
}
