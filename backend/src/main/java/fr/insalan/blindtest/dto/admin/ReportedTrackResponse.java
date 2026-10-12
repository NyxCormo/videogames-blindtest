package fr.insalan.blindtest.dto.admin;

import java.util.List;

import fr.insalan.blindtest.dto.TrackResponse;

public record ReportedTrackResponse(
    TrackResponse track,
    List<String> reporters
) {
}
