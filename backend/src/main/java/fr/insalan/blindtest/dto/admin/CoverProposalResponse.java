package fr.insalan.blindtest.dto.admin;

import java.time.Instant;

import fr.insalan.blindtest.cover.CoverUrls;
import fr.insalan.blindtest.dto.GameResponse;
import fr.insalan.blindtest.model.CoverProposal;

public record CoverProposalResponse(
    GameResponse game,
    String currentUrl,
    String imageUrl,
    Integer listenerId,
    String listenerName,
    Instant createdAt
) {
    public static CoverProposalResponse from(CoverProposal proposal) {
        return new CoverProposalResponse(
            GameResponse.from(proposal.getGame()),
            CoverUrls.publicUrl(proposal.getGame()),
            proposal.getImageUrl(),
            proposal.getListener().getId(),
            proposal.getListener().getName(),
            proposal.getCreatedAt()
        );
    }
}
