package fr.insalan.blindtest.cover;

import java.time.Instant;
import java.util.List;

import fr.insalan.blindtest.dto.GameResponse;

public record AlbumOverview(
    String albumUrl,
    String pageUrl,
    String name,
    List<GameResponse> games,
    List<String> tracks,
    Instant scannedAt,
    Instant pageSavedAt,
    List<String> images
) {}
