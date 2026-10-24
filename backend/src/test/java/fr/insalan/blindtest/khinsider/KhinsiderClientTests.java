package fr.insalan.blindtest.khinsider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
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

    @Test
    void extractsTheFullSizeImagesOfTheAlbumFromARealPage() throws IOException {
        Document page = parseFixture("stellar-blade-album.html");

        assertEquals(
                List.of(
                        "https://jetta.vgmtreasurechest.com/soundtracks/stellar-blade-soundtrack-2024/00%20Front.jpg",
                        "https://jetta.vgmtreasurechest.com/soundtracks/stellar-blade-soundtrack-2024/folder.jpg"),
                KhinsiderClient.extractAlbumImages(page, "stellar-blade-soundtrack-2024"));
    }

    @Test
    void findsNoAlbumImageOnASongPage() throws IOException {
        Document page = parseFixture("dawn.html");

        assertEquals(List.of(), KhinsiderClient.extractAlbumImages(page, "stellar-blade-soundtrack-2024"));
    }

    @Test
    void albumUrlIsTheSongUrlWithoutTheFileName() {
        assertEquals(
                "https://downloads.khinsider.com/game-soundtracks/album/stellar-blade-soundtrack-2024",
                KhinsiderClient.albumUrlOf("https://downloads.khinsider.com/game-soundtracks/album/stellar-blade-soundtrack-2024/02.%2520Shelter%2520%2528Title%2529.mp3"));
    }

    private Document parseFixture(String fileName) throws IOException {
        try (InputStream html = getClass().getResourceAsStream("/khinsider/" + fileName)) {
            return Jsoup.parse(html, "UTF-8", "https://downloads.khinsider.com/");
        }
    }
}
