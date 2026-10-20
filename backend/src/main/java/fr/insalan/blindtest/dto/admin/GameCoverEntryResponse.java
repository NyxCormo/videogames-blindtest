package fr.insalan.blindtest.dto.admin;

import fr.insalan.blindtest.cover.CoverUrls;
import fr.insalan.blindtest.model.Game;

public record GameCoverEntryResponse(
    Integer gameId,
    String displayUrl
) {
    public static GameCoverEntryResponse from(Game game) {
        return new GameCoverEntryResponse(game.getId(), CoverUrls.publicUrl(game));
    }
}
