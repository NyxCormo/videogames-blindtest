package fr.insalan.blindtest.cover;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import fr.insalan.blindtest.khinsider.KhinsiderClient;
import fr.insalan.blindtest.khinsider.RequestPacer;

@Component
public class CoverFinder {

    static final List<String> COMMON_NAMES = List.of(
        "00 Front.jpg",
        "00. Front.jpg",
        "Front.jpg",
        "front.jpg",
        "Cover.jpg",
        "cover.jpg",
        "00 Cover.jpg",
        "folder.jpg"
    );
    private static final Pattern AUDIO_IN_ALBUM = Pattern.compile("(.+/soundtracks/[^/]+)/[^/]+/[^/]+");
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final RequestPacer pacer;

    public CoverFinder(RequestPacer pacer) {
        this.pacer = pacer;
    }

    public static Optional<String> albumOf(String audioLink) {
        Matcher matcher = AUDIO_IN_ALBUM.matcher(audioLink);
        return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    public Optional<String> findFirst(String album) {
        return COMMON_NAMES.stream()
            .map(name -> album + "/" + name.replace(" ", "%20"))
            .filter(this::isImage)
            .findFirst();
    }

    public List<String> findAll(String album) {
        List<String> images = new ArrayList<>();
        for (String name : COMMON_NAMES) {
            String url = album + "/" + name.replace(" ", "%20");
            if (isImage(url)) {
                images.add(url);
            }
        }
        return images;
    }

    private boolean isImage(String url) {
        pacer.waitForTurn();
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .header("User-Agent", KhinsiderClient.USER_AGENT)
                .timeout(TIMEOUT)
                .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() == 200
                && response.headers().firstValue("Content-Type").orElse("").startsWith("image/");
        } catch (IOException | IllegalArgumentException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
