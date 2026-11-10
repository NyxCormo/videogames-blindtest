package fr.insalan.blindtest.controller;

import java.nio.file.Path;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import fr.insalan.blindtest.repository.TrackRepository;
import fr.insalan.blindtest.youtube.YoutubeAudioStore;

@RestController
public class TrackAudioController {

    private final TrackRepository trackRepository;
    private final YoutubeAudioStore youtubeAudioStore;

    public TrackAudioController(TrackRepository trackRepository, YoutubeAudioStore youtubeAudioStore) {
        this.trackRepository = trackRepository;
        this.youtubeAudioStore = youtubeAudioStore;
    }

    @GetMapping("/api/tracks/{id}/audio")
    public ResponseEntity<Resource> audio(@PathVariable Integer id) {
        Path file = trackRepository.findById(id)
            .flatMap(track -> youtubeAudioStore.find(track.getYoutubeAudioFile()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pas d'audio téléchargé pour cette musique"));
        Resource audio = new FileSystemResource(file);
        return ResponseEntity.ok()
            .contentType(MediaTypeFactory.getMediaType(audio).orElse(MediaType.APPLICATION_OCTET_STREAM))
            .cacheControl(CacheControl.noCache())
            .body(audio);
    }
}
