package fr.insalan.blindtest.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import fr.insalan.blindtest.dto.AddTagRequest;
import fr.insalan.blindtest.dto.GameResponse;
import fr.insalan.blindtest.dto.TrackResponse;
import fr.insalan.blindtest.repository.GameRepository;
import fr.insalan.blindtest.repository.TrackRepository;
import fr.insalan.blindtest.tag.TagService;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameRepository gameRepository;
    private final TrackRepository trackRepository;
    private final TagService tagService;

    public GameController(GameRepository gameRepository, TrackRepository trackRepository, TagService tagService) {
        this.gameRepository = gameRepository;
        this.trackRepository = trackRepository;
        this.tagService = tagService;
    }

    // Tous les jeux, avec leur franchise : la case "Jeu" de la réponse à un blindtest cherche dedans.
    @GetMapping
    public List<GameResponse> list() {
        return gameRepository.findAllWithFranchise().stream()
            .map(GameResponse::from)
            .toList();
    }

    @GetMapping("/{id}/tracks")
    public List<TrackResponse> tracks(@PathVariable Integer id) {
        return trackRepository.findByGameIdWithGameAndFranchise(id).stream()
            .map(TrackResponse::from)
            .toList();
    }

    // Applique un tag à toutes les musiques du jeu (tags inhérents au jeu : genre, plateforme).
    @PostMapping("/{id}/tags")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void applyTagToGame(@PathVariable Integer id, @RequestBody AddTagRequest request) {
        tagService.applyToGame(request.tagId(), id);
    }
}
