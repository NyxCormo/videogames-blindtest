package fr.insalan.blindtest.dto.admin;

import fr.insalan.blindtest.cover.CoverUrls;
import fr.insalan.blindtest.model.Game;

public record AdminCoverResponse(
    String coverUrl,
    String displayUrl,
    boolean downloaded
) {
    public static AdminCoverResponse from(Game game, boolean downloaded) {
        return new AdminCoverResponse(game.getCoverUrl(), CoverUrls.publicUrl(game), downloaded);
    }
}
