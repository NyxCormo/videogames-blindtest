package fr.insalan.blindtest.dto;

import java.time.Instant;

import fr.insalan.blindtest.model.Blindtest;

public record BlindtestResponse(
    Integer id,
    String name,
    int difficulty,
    Instant createdAt
) {
    public static BlindtestResponse from(Blindtest blindtest) {
        return new BlindtestResponse(
            blindtest.getId(),
            blindtest.getName(),
            blindtest.getDifficulty(),
            blindtest.getCreatedAt()
        );
    }
}
