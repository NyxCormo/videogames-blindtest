package fr.insalan.blindtest.model;

import java.io.Serializable;

import jakarta.persistence.Embeddable;

// Clé primaire composée de la table knowledge
@Embeddable 
public record KnowledgeId(
    Integer listenerId,
    Integer trackId
) implements Serializable {
}
