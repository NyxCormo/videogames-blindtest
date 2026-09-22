package fr.insalan.blindtest.model;

import java.io.Serializable;

import jakarta.persistence.Embeddable;

// Clé primaire composée de la table blindtest_track
@Embeddable
public record BlindtestTrackId(
    Integer blindtestId,
    Integer position
) implements Serializable {
}
