package fr.insalan.blindtest.model;

import java.io.Serializable;

import jakarta.persistence.Embeddable;

// Clé primaire composée de la table track_tag
@Embeddable
public record TrackTagId(
    Integer trackId,
    Integer tagId
) implements Serializable {
}
