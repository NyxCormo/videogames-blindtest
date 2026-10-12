package fr.insalan.blindtest.dto;

public record TrackReportStatusResponse(
    long reports,
    boolean reportedByListener
) {
}
