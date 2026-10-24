package fr.insalan.blindtest.cover;

import fr.insalan.blindtest.model.AlbumScan;

public enum CoverFillMode {
    NEW,
    WITHOUT_IMAGE,
    ALL;

    public boolean needsScan(AlbumScan scan) {
        return switch (this) {
            case NEW -> scan == null;
            case WITHOUT_IMAGE -> scan == null || scan.getImages().isEmpty();
            case ALL -> true;
        };
    }
}
