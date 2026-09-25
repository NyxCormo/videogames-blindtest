package fr.insalan.blindtest.dto;

import java.util.List;

public record CreateBlindtestRequest(
    String name,
    int trackCount,
    List<DifficultyBandRequest> difficultyBands,
    List<Integer> tagIds,
    Boolean matchAllTags,
    Integer maxPerGame,
    Integer maxPerFranchise,
    String strategy
) {
}
