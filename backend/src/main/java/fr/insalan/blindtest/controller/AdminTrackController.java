package fr.insalan.blindtest.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import fr.insalan.blindtest.dto.SetLinkRequest;
import fr.insalan.blindtest.dto.TrackResponse;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.repository.TrackRepository;
import fr.insalan.blindtest.track.TrackLinkService;

@RestController
@RequestMapping("/api/admin/tracks")
public class AdminTrackController {

    private final TrackRepository trackRepository;
    private final TrackLinkService trackLinkService;

    public AdminTrackController(TrackRepository trackRepository, TrackLinkService trackLinkService) {
        this.trackRepository = trackRepository;
        this.trackLinkService = trackLinkService;
    }

    @PutMapping("/{id}/khinsider-link")
    public TrackResponse replaceKhinsiderLink(@PathVariable Integer id, @RequestBody SetLinkRequest request) {
        Track track = findTrack(id);
        trackLinkService.setKhinsiderLink(track, request.link());
        return TrackResponse.from(track);
    }

    @DeleteMapping("/{id}/khinsider-link")
    public TrackResponse removeKhinsiderLink(@PathVariable Integer id) {
        Track track = findTrack(id);
        trackLinkService.removeKhinsiderLink(track);
        return TrackResponse.from(track);
    }

    @PutMapping("/{id}/youtube-link")
    public TrackResponse replaceYoutubeLink(@PathVariable Integer id, @RequestBody SetLinkRequest request) {
        Track track = findTrack(id);
        trackLinkService.setYoutubeLink(track, request.link());
        return TrackResponse.from(track);
    }

    @DeleteMapping("/{id}/youtube-link")
    public TrackResponse removeYoutubeLink(@PathVariable Integer id) {
        Track track = findTrack(id);
        trackLinkService.removeYoutubeLink(track);
        return TrackResponse.from(track);
    }

    private Track findTrack(Integer id) {
        return trackRepository.findByIdWithGameAndFranchise(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Musique introuvable"));
    }
}
