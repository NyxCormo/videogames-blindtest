package fr.insalan.blindtest.model;

import java.io.Serializable;

import jakarta.persistence.Embeddable;

// Clé primaire composée de la table blindtest_difficulty_band
@Embeddable
public record BlindtestDifficultyBandId(
    Integer blindtestId,
    Integer position
) implements Serializable {
}
