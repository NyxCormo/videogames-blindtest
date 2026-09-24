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
import fr.insalan.blindtest.dto.TagResponse;
import fr.insalan.blindtest.dto.TrackResponse;
import fr.insalan.blindtest.model.Tag;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.model.TrackTag;
import fr.insalan.blindtest.model.TrackTagId;
import fr.insalan.blindtest.repository.TagRepository;
import fr.insalan.blindtest.repository.TrackRepository;
import fr.insalan.blindtest.repository.TrackTagRepository;

@RestController 
@RequestMapping("/api/tracks")
public class TrackController {

    private final TrackRepository trackRepository;
    private final TagRepository tagRepository;
    private final TrackTagRepository trackTagRepository;

    public TrackController(
        TrackRepository trackRepository,
        TagRepository tagRepository,
        TrackTagRepository trackTagRepository
    ) {
        this.trackRepository = trackRepository;
        this.tagRepository = tagRepository;
        this.trackTagRepository = trackTagRepository;
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