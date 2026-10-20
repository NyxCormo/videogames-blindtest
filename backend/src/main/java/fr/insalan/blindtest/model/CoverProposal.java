package fr.insalan.blindtest.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;

@Entity
public class CoverProposal {

    @EmbeddedId
    private CoverProposalId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("gameId")
    @JoinColumn(name = "game_id")
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("listenerId")
    @JoinColumn(name = "listener_id")
    private Listener listener;

    private String imageUrl;

    @Column(nullable = false, columnDefinition = "datetime")
    private Instant createdAt;

    protected CoverProposal() {
        // JPA
    }

    public CoverProposal(Game game, Listener listener, String imageUrl) {
        this.id = new CoverProposalId(game.getId(), listener.getId());
        this.game = game;
        this.listener = listener;
        this.imageUrl = imageUrl;
        this.createdAt = Instant.now();
    }

    public Game getGame() {
        return game;
    }

    public Listener getListener() {
        return listener;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
