package fr.insalan.blindtest.cover;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AlbumPageStore {

    private final Path directory;

    public AlbumPageStore(@Value("${khinsider.pages-directory:data/khinsider-pages}") String directory) {
        this.directory = Path.of(directory);
    }

    public void save(String slug, String html) {
        try {
            Files.createDirectories(directory);
            Files.writeString(file(slug), html);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Optional<String> find(String slug) {
        try {
            Path file = file(slug);
            return Files.exists(file) ? Optional.of(Files.readString(file)) : Optional.empty();
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Optional<Instant> savedAt(String slug) {
        try {
            Path file = file(slug);
            return Files.exists(file) ? Optional.of(Files.getLastModifiedTime(file).toInstant()) : Optional.empty();
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void deleteAll() {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (Stream<Path> files = Files.list(directory)) {
            for (Path file : files.toList()) {
                Files.delete(file);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path file(String slug) {
        if (slug.isBlank() || slug.startsWith(".") || slug.contains("/") || slug.contains("\\")) {
            throw new IllegalArgumentException("Nom d'album invalide : " + slug);
        }
        try {
            Path file = directory.resolve(slug + ".html").normalize();
            if (!directory.normalize().equals(file.getParent())) {
                throw new IllegalArgumentException("Nom d'album invalide : " + slug);
            }
            return file;
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("Nom d'album invalide : " + slug, e);
        }
    }
}
