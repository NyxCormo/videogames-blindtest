package fr.insalan.blindtest.audiolink;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.stereotype.Component;

import fr.insalan.blindtest.khinsider.RequestPacer;

@Component 
public class AudioLinkChecker {
    
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final RequestPacer pacer;

    public AudioLinkChecker(RequestPacer pacer){
        this.pacer = pacer;
    }

    public boolean isAlive(String url) {
        pacer.waitForTurn();
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .timeout(TIMEOUT)
                .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() / 100 == 2;
        } catch (IOException e) {
            // hôte injoignable, lien expiré, ... -> lien mort pour plus de simplicité
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
