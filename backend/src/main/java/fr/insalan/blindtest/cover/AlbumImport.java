package fr.insalan.blindtest.cover;

import java.util.List;

public record AlbumImport(
    String album,
    List<String> images,
    List<String> coveredGames
) {}
