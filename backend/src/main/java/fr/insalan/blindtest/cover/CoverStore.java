package fr.insalan.blindtest.cover;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fr.insalan.blindtest.khinsider.KhinsiderClient;
import fr.insalan.blindtest.khinsider.RequestPacer;

@Component
public class CoverStore {

    private static final Logger log = LoggerFactory.getLogger(CoverStore.class);
    private static final int MAX_BYTES = 5_000_000;
    private static final Map<String, String> EXTENSIONS = Map.of(
        "image/jpeg", "jpg",
        "image/png", "png",
        "image/gif", "gif",
        "image/webp", "webp"
    );

    private final Path directory;
    private final boolean downloadEnabled;
    private final RequestPacer pacer;
    private final HttpClient http = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    public CoverStore(
        @Value("${covers.directory:data/covers}") String directory,
        @Value("${covers.download:true}") boolean downloadEnabled,
        RequestPacer pacer
    ) {
        this.directory = Path.of(directory);
        this.downloadEnabled = downloadEnabled;
        this.pacer = pacer;
    }

    public Optional<Path> find(Integer gameId) {
        return EXTENSIONS.values().stream()
            .map(extension -> directory.resolve(gameId + "." + extension))
            .filter(Files::exists)
            .findFirst();
    }

    public void delete(Integer gameId) {
        for (String extension : EXTENSIONS.values()) {
            try {
                Files.deleteIfExists(directory.resolve(gameId + "." + extension));
            } catch (IOException e) {
                log.warn("Impossible de supprimer l'image du jeu {} : {}", gameId, e.getMessage());
            }
        }
    }

    public boolean download(Integer gameId, String url) {
        if (!downloadEnabled) {
            return false;
        }
        try {
            pacer.waitForTurn();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", KhinsiderClient.USER_AGENT)
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();
            HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            String type = response.headers().firstValue("Content-Type").orElse("").split(";")[0].trim().toLowerCase();
            String extension = EXTENSIONS.get(type);
            if (response.statusCode() != 200 || extension == null || response.body().length > MAX_BYTES) {
                log.warn("Image du jeu {} refusée ({}, {}, {} octets)", gameId, response.statusCode(), type, response.body().length);
                return false;
            }
            Files.createDirectories(directory);
            delete(gameId);
            Files.write(directory.resolve(gameId + "." + extension), response.body());
            return true;
        } catch (IOException | IllegalArgumentException e) {
            log.warn("Impossible de télécharger l'image du jeu {} : {}", gameId, e.getMessage());
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
