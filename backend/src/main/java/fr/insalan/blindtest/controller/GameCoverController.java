package fr.insalan.blindtest.controller;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import fr.insalan.blindtest.cover.CoverStore;
import fr.insalan.blindtest.model.Game;
import fr.insalan.blindtest.repository.GameRepository;

@RestController
@RequestMapping("/api/games/{id}")
public class GameCoverController {

    private final GameRepository gameRepository;
    private final CoverStore coverStore;

    public GameCoverController(GameRepository gameRepository, CoverStore coverStore) {
        this.gameRepository = gameRepository;
        this.coverStore = coverStore;
    }

    @GetMapping("/cover")
    public ResponseEntity<Resource> cover(@PathVariable Integer id) {
        Game game = gameRepository.findById(id)
            .filter(found -> found.getCoverUrl() != null)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pas d'image pour ce jeu"));
        Optional<Path> file = coverStore.find(id);
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(game.getCoverUrl())).build();
        }
        Resource image = new FileSystemResource(file.get());
        return ResponseEntity.ok()
            .contentType(MediaTypeFactory.getMediaType(image).orElse(MediaType.APPLICATION_OCTET_STREAM))
            .cacheControl(CacheControl.maxAge(Duration.ofDays(7)))
            .body(image);
    }

}
