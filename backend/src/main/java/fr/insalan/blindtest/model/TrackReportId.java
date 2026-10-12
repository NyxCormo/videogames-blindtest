package fr.insalan.blindtest.model;

import java.io.Serializable;

import jakarta.persistence.Embeddable;

// Clé primaire composée de la table track_report
@Embeddable
public record TrackReportId(
    Integer trackId,
    Integer listenerId
) implements Serializable {
}
