package fr.insalan.blindtest.khinsider;

import java.io.IOException;
import java.util.Optional;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

// Lit une page de musique KHInsider pour en extraire le lien audio direct.
@Component
public class KhinsiderClient {

    private static final String USER_AGENT = "blindtest-insalan (French Student Association, https://github.com/NyxCormo/videogames-blindtest)";
    private static final int TIMEOUT_MS = 10_000;

    private final RequestPacer pacer;

    public KhinsiderClient(RequestPacer pacer) {
        this.pacer = pacer;
    }

    public Optional<String> resolveAudioLink(String pageUrl) throws IOException {
        pacer.waitForTurn();
        Document page = Jsoup.connect(pageUrl)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .get();
        return extractAudioLink(page);
    }

    static Optional<String> extractAudioLink(Document page) {
        Element audio = page.selectFirst("audio#audio");
        if (audio == null) {
            return Optional.empty();
        }
        String src = audio.attr("src");
        return src.isBlank() ? Optional.empty() : Optional.of(src);
    }
}
