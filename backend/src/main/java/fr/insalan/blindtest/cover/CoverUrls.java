package fr.insalan.blindtest.cover;

import fr.insalan.blindtest.model.Game;

public final class CoverUrls {

    private CoverUrls() {
    }

    public static String publicUrl(Game game) {
        if (game.getCoverUrl() == null) {
            return null;
        }
        return "/api/games/" + game.getId() + "/cover?v=" + Integer.toHexString(game.getCoverUrl().hashCode());
    }
}
