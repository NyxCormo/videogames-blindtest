package fr.insalan.blindtest.model;

import java.io.Serializable;

import jakarta.persistence.Embeddable;

// Clé primaire composée de la table blindtest_score
@Embeddable
public record BlindtestScoreId(
    Integer blindtestId,
    Integer listenerId
) implements Serializable {
}
