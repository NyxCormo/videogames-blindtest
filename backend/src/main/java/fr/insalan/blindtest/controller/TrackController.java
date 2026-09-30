package fr.insalan.blindtest.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import fr.insalan.blindtest.dto.AddTagRequest;
import fr.insalan.blindtest.dto.CreateTrackRequest;
import fr.insalan.blindtest.dto.SetLinkRequest;
import fr.insalan.blindtest.dto.TagResponse;
import fr.insalan.blindtest.dto.TrackResponse;
import fr.insalan.blindtest.model.Game;
import fr.insalan.blindtest.model.Tag;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.model.TrackTag;
import fr.insalan.blindtest.model.TrackTagId;
import fr.insalan.blindtest.repository.GameRepository;
import fr.insalan.blindtest.repository.TagRepository;
import fr.insalan.blindtest.repository.TrackRepository;
import fr.insalan.blindtest.repository.TrackTagRepository;
import fr.insalan.blindtest.track.TrackLinkService;

@RestController
@RequestMapping("/api/tracks")
public class TrackController {

    private final TrackRepository trackRepository;
    private final GameRepository gameRepository;
    private final TagRepository tagRepository;
    private final TrackTagRepository trackTagRepository;
    private final TrackLinkService trackLinkService;

    public TrackController(
        TrackRepository trackRepository,
        GameRepository gameRepository,
        TagRepository tagRepository,
        TrackTagRepository trackTagRepository,
        TrackLinkService trackLinkService
    ) {
        this.trackRepository = trackRepository;
        this.gameRepository = gameRepository;
        this.tagRepository = tagRepository;
        this.trackTagRepository = trackTagRepository;
        this.trackLinkService = trackLinkService;
    }

    @GetMapping 
    public List<TrackResponse> list() {
        return trackRepository.findAllWithGameAndFranchise().stream()
            .map(TrackResponse::from)
            .toList();
    }

    @GetMapping("/{id}")
    public TrackResponse get(@PathVariable Integer id) {
        return trackRepository.findByIdWithGameAndFranchise(id)
            .map(TrackResponse::from)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Musique introuvable"));
    }

    // Retrouve la musique si elle existe déjà pour ce jeu (même principe que l'import du Google Sheet), sinon la crée.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TrackResponse create(@RequestBody CreateTrackRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom est obligatoire");
        }
        if (request.gameId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le jeu est obligatoire");
        }
        Game game = gameRepository.findByIdWithFranchise(request.gameId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Jeu inconnu"));
        String name = request.name().trim();
        if (trackRepository.existsByGameAndNameIgnoreCase(game, name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La musique « " + name + " » existe déjà dans ce jeu");
        }
        return TrackResponse.from(trackRepository.save(new Track(name, game)));
    }

    // Refuse d'écraser un lien déjà présent : le remplacer ou le retirer se fait dans le panneau admin.
    @PostMapping("/{id}/khinsider-link")
    public TrackResponse setKhinsiderLink(@PathVariable Integer id, @RequestBody SetLinkRequest request) {
        Track track = trackRepository.findByIdWithGameAndFranchise(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Musique introuvable"));
        if (track.getKhinsiderLink() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un lien KHInsider existe déjà pour cette musique");
        }
        trackLinkService.setKhinsiderLink(track, request.link());
        return TrackResponse.from(track);
    }

    @PostMapping("/{id}/youtube-link")
    public TrackResponse setYoutubeLink(@PathVariable Integer id, @RequestBody SetLinkRequest request) {
        Track track = trackRepository.findByIdWithGameAndFranchise(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Musique introuvable"));
        if (track.getYoutubeLink() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un lien YouTube existe déjà pour cette musique");
        }
        trackLinkService.setYoutubeLink(track, request.link());
        return TrackResponse.from(track);
    }

    @GetMapping("/{id}/tags")
    public List<TagResponse> tags(@PathVariable Integer id) {
        return trackTagRepository.findWithTagByTrackId(id).stream()
            .map(trackTag -> TagResponse.from(trackTag.getTag()))
            .toList();
    }

    @PostMapping("/{id}/tags")
    @ResponseStatus(HttpStatus.CREATED)
    public void addTag(@PathVariable Integer id, @RequestBody AddTagRequest request) {
        TrackTagId trackTagId = new TrackTagId(id, request.tagId());
        if (trackTagRepository.existsById(trackTagId)) {
            return;
        }
        Track track = trackRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Musique introuvable"));
        Tag tag = tagRepository.findById(request.tagId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tag inconnu"));
        trackTagRepository.save(new TrackTag(track, tag));
    }

    @DeleteMapping("/{id}/tags/{tagId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeTag(@PathVariable Integer id, @PathVariable Integer tagId) {
        TrackTagId trackTagId = new TrackTagId(id, tagId);
        if (trackTagRepository.existsById(trackTagId)) {
            trackTagRepository.deleteById(trackTagId);
        }
    }
}