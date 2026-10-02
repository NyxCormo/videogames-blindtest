package fr.insalan.blindtest.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import fr.insalan.blindtest.deletion.DeletePreview;
import fr.insalan.blindtest.deletion.DeletionService;
import fr.insalan.blindtest.dto.MergeRequest;
import fr.insalan.blindtest.dto.RenameRequest;
import fr.insalan.blindtest.dto.SetLinkRequest;
import fr.insalan.blindtest.dto.TrackResponse;
import fr.insalan.blindtest.merge.MergePreview;
import fr.insalan.blindtest.merge.MergeService;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.repository.TrackRepository;
import fr.insalan.blindtest.track.TrackLinkService;

@RestController
@RequestMapping("/api/admin/tracks")
public class AdminTrackController {

    private final TrackRepository trackRepository;
    private final TrackLinkService trackLinkService;
    private final MergeService mergeService;
    private final DeletionService deletionService;

    public AdminTrackController(
        TrackRepository trackRepository,
        TrackLinkService trackLinkService,
        MergeService mergeService,
        DeletionService deletionService
    ) {
        this.trackRepository = trackRepository;
        this.trackLinkService = trackLinkService;
        this.mergeService = mergeService;
        this.deletionService = deletionService;
    }

    @PatchMapping("/{id}")
    public TrackResponse rename(@PathVariable Integer id, @RequestBody RenameRequest request) {
        Track track = findTrack(id);
        String name = AdminNames.require(request.name());
        if (trackRepository.existsByGameAndNameIgnoreCaseAndIdNot(track.getGame(), name, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La musique « " + name + " » existe déjà dans ce jeu");
        }
        track.setName(name);
        trackRepository.save(track);
        return TrackResponse.from(track);
    }

    @GetMapping("/{id}/merge-preview")
    public MergePreview mergePreview(@PathVariable Integer id, @RequestParam Integer into) {
        return mergeService.previewTracks(id, into);
    }

    @PostMapping("/{id}/merge")
    public TrackResponse merge(@PathVariable Integer id, @RequestBody MergeRequest request) {
        mergeService.mergeTracks(id, request.targetId());
        return TrackResponse.from(findTrack(request.targetId()));
    }

    @GetMapping("/{id}/delete-preview")
    public DeletePreview deletePreview(@PathVariable Integer id) {
        return deletionService.previewTrack(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        deletionService.deleteTrack(id);
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
