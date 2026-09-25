package fr.insalan.blindtest.dto;

import fr.insalan.blindtest.model.Game;

public record GameResponse(
    Integer id,
    String name,
    String franchiseName
) {
    public static GameResponse from(Game game) {
        return new GameResponse(game.getId(), game.getName(), game.getFranchise().getName());
    }
}
