package fr.insalan.blindtest.youtube;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class YoutubeAudioStore {

    private static final Set<String> EXTENSIONS = Set.of("m4a", "webm", "opus", "ogg", "mp3", "aac");

    private final Path directory;
    private final long maxBytes;

    public YoutubeAudioStore(
        @Value("${youtube.audio-directory:data/youtube-audio}") String directory,
        @Value("${youtube.max-file-size-mb:50}") long maxMegabytes
    ) {
        this.directory = Path.of(directory);
        this.maxBytes = maxMegabytes * 1024 * 1024;
    }

    public Path newDownloadDirectory() throws IOException {
        Files.createDirectories(directory);
        return Files.createTempDirectory(directory, "telechargement-");
    }

    public String keep(Integer trackId, Path downloaded) throws IOException {
        if (downloaded == null || !Files.isRegularFile(downloaded)) {
            throw new IOException("Le téléchargeur n'a renvoyé aucun fichier");
        }
        String name = downloaded.getFileName().toString();
        String extension = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!name.contains(".") || !EXTENSIONS.contains(extension)) {
            throw new IOException("Format audio non accepté : " + name);
        }
        long size = Files.size(downloaded);
        if (size == 0 || size > maxBytes) {
            throw new IOException("Taille du fichier refusée : " + size + " octets");
        }
        String fileName = trackId + "." + extension;
        delete(trackId);
        Files.move(downloaded, directory.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        return fileName;
    }

    public Optional<Path> find(String fileName) {
        if (fileName == null) {
            return Optional.empty();
        }
        Path file = directory.resolve(fileName);
        return Files.isRegularFile(file) ? Optional.of(file) : Optional.empty();
    }

    public void delete(Integer trackId) throws IOException {
        for (String extension : EXTENSIONS) {
            Files.deleteIfExists(directory.resolve(trackId + "." + extension));
        }
    }

    public void deleteDirectory(Path temporary) throws IOException {
        if (temporary == null || !Files.exists(temporary)) {
            return;
        }
        try (Stream<Path> files = Files.walk(temporary)) {
            for (Path file : files.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(file);
            }
        }
    }
}
