package fr.insalan.blindtest.model;

import java.io.Serializable;

import jakarta.persistence.Embeddable;

@Embeddable
public record CoverProposalId(
    Integer gameId,
    Integer listenerId
) implements Serializable {
}
