package fr.insalan.blindtest.controller;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import fr.insalan.blindtest.audiolink.AudioLinkRefreshService;
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

@RestController
@RequestMapping("/api/tracks")
public class TrackController {

    private static final Logger log = LoggerFactory.getLogger(TrackController.class);

    private final TrackRepository trackRepository;
    private final GameRepository gameRepository;
    private final TagRepository tagRepository;
    private final TrackTagRepository trackTagRepository;
    private final AudioLinkRefreshService audioLinkRefreshService;

    public TrackController(
        TrackRepository trackRepository,
        GameRepository gameRepository,
        TagRepository tagRepository,
        TrackTagRepository trackTagRepository,
        AudioLinkRefreshService audioLinkRefreshService
    ) {
        this.trackRepository = trackRepository;
        this.gameRepository = gameRepository;
        this.tagRepository = tagRepository;
        this.trackTagRepository = trackTagRepository;
        this.audioLinkRefreshService = audioLinkRefreshService;
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
        if (trackRepository.findByGameAndName(game, request.name()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La musique « " + request.name() + " » existe déjà dans ce jeu");
        }
        return TrackResponse.from(trackRepository.save(new Track(request.name(), game)));
    }

    // Refuse d'écraser un lien déjà présent : la correction d'un lien existant sera réservée à un futur panneau admin.
    @PostMapping("/{id}/khinsider-link")
    public TrackResponse setKhinsiderLink(@PathVariable Integer id, @RequestBody SetLinkRequest request) {
        requireHttpLink(request.link());
        if (!request.link().contains("khinsider.com")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le lien doit pointer vers khinsider.com");
        }
        Track track = trackRepository.findByIdWithGameAndFranchise(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Musique introuvable"));
        if (track.getKhinsiderLink() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un lien KHInsider existe déjà pour cette musique");
        }
        track.setKhinsiderLink(request.link());
        trackRepository.save(track);
        try {
            audioLinkRefreshService.refreshOne(track);
        } catch (IOException e) {
            log.warn("Impossible de résoudre le lien audio pour la musique {} : {}", track.getId(), e.getMessage());
        }
        return TrackResponse.from(track);
    }

    @PostMapping("/{id}/youtube-link")
    public TrackResponse setYoutubeLink(@PathVariable Integer id, @RequestBody SetLinkRequest request) {
        requireHttpLink(request.link());
        if (!request.link().contains("youtube.com") && !request.link().contains("youtu.be")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le lien doit pointer vers youtube.com ou youtu.be");
        }
        Track track = trackRepository.findByIdWithGameAndFranchise(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Musique introuvable"));
        if (track.getYoutubeLink() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un lien YouTube existe déjà pour cette musique");
        }
        track.setYoutubeLink(request.link());
        trackRepository.save(track);
        return TrackResponse.from(track);
    }

    // Un lien affiché tel quel dans un <a href> côté front : refuser tout ce qui n'est pas http(s)
    // évite qu'un lien "javascript:..." s'exécute au clic.
    private void requireHttpLink(String link) {
        if (link == null || link.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le lien est obligatoire");
        }
        if (!link.startsWith("http://") && !link.startsWith("https://")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le lien doit commencer par http:// ou https://");
        }
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
        Track track = trackRepository.findById(id).orElseThrow();
        Tag tag = tagRepository.findById(request.tagId()).orElseThrow();
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