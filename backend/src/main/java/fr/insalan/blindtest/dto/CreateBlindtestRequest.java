package fr.insalan.blindtest.dto;

import java.util.List;

public record CreateBlindtestRequest(
    String name,
    int trackCount,
    int difficulty,
    List<Integer> tagIds,
    Boolean matchAllTags
) {
}
