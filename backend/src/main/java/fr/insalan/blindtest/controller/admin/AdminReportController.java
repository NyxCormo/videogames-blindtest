package fr.insalan.blindtest.controller.admin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import fr.insalan.blindtest.dto.TrackResponse;
import fr.insalan.blindtest.dto.admin.ReportedTrackResponse;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.model.TrackReport;
import fr.insalan.blindtest.repository.TrackReportRepository;

@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportController {

    private final TrackReportRepository trackReportRepository;

    public AdminReportController(TrackReportRepository trackReportRepository) {
        this.trackReportRepository = trackReportRepository;
    }

    @GetMapping
    public List<ReportedTrackResponse> list() {
        Map<Integer, Track> tracks = new LinkedHashMap<>();
        Map<Integer, List<String>> reporters = new LinkedHashMap<>();
        for (TrackReport report : trackReportRepository.findAllWithTrackAndListener()) {
            Track track = report.getTrack();
            tracks.put(track.getId(), track);
            reporters.computeIfAbsent(track.getId(), id -> new ArrayList<>()).add(report.getListener().getName());
        }
        return tracks.values().stream()
            .map(track -> new ReportedTrackResponse(TrackResponse.from(track), reporters.get(track.getId())))
            .toList();
    }

    @DeleteMapping("/{trackId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void dismiss(@PathVariable Integer trackId) {
        trackReportRepository.deleteByTrackId(trackId);
    }
}
