package fr.insalan.blindtest.khinsider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

class KhinsiderClientTests {

    @Test
    void extractsTheAudioLinkFromARealPage() throws IOException {
        Document page = parseFixture("dawn.html");

        Optional<String> audioLink = KhinsiderClient.extractAudioLink(page);

        assertEquals(
                Optional.of("https://jetta.vgmtreasurechest.com/soundtracks/stellar-blade-soundtrack-2024/ybomulwy/62.%20Dawn.mp3"),
                audioLink);
    }

    @Test
    void returnsEmptyWhenThePageHasNoAudioElement() {
        Document page = Jsoup.parse("<html><body>Musique indisponible</body></html>");

        assertEquals(Optional.empty(), KhinsiderClient.extractAudioLink(page));
    }

    private Document parseFixture(String fileName) throws IOException {
        try (InputStream html = getClass().getResourceAsStream("/khinsider/" + fileName)) {
            return Jsoup.parse(html, "UTF-8", "https://downloads.khinsider.com/");
        }
    }
}
