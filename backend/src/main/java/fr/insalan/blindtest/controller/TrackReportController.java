package fr.insalan.blindtest.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import fr.insalan.blindtest.dto.TrackReportStatusResponse;
import fr.insalan.blindtest.model.Listener;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.model.TrackReport;
import fr.insalan.blindtest.model.TrackReportId;
import fr.insalan.blindtest.repository.ListenerRepository;
import fr.insalan.blindtest.repository.TrackReportRepository;
import fr.insalan.blindtest.repository.TrackRepository;

@RestController
@RequestMapping("/api/tracks/{trackId}/reports/{listenerId}")
public class TrackReportController {

    private final TrackRepository trackRepository;
    private final ListenerRepository listenerRepository;
    private final TrackReportRepository trackReportRepository;

    public TrackReportController(
        TrackRepository trackRepository,
        ListenerRepository listenerRepository,
        TrackReportRepository trackReportRepository
    ) {
        this.trackRepository = trackRepository;
        this.listenerRepository = listenerRepository;
        this.trackReportRepository = trackReportRepository;
    }

    @GetMapping
    public TrackReportStatusResponse status(@PathVariable Integer trackId, @PathVariable Integer listenerId) {
        return new TrackReportStatusResponse(
            trackReportRepository.countByTrackId(trackId),
            trackReportRepository.existsById(new TrackReportId(trackId, listenerId))
        );
    }

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void report(@PathVariable Integer trackId, @PathVariable Integer listenerId) {
        Track track = trackRepository.findById(trackId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Musique introuvable"));
        Listener listener = listenerRepository.findById(listenerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pseudo introuvable"));
        if (!trackReportRepository.existsById(new TrackReportId(trackId, listenerId))) {
            trackReportRepository.save(new TrackReport(track, listener));
        }
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Integer trackId, @PathVariable Integer listenerId) {
        trackReportRepository.findById(new TrackReportId(trackId, listenerId)).ifPresent(trackReportRepository::delete);
    }
}
