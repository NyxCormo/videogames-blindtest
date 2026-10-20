package fr.insalan.blindtest.cover;

import java.util.List;

public record AlbumImages(
    String albumUrl,
    List<String> tracks,
    List<String> images
) {}
