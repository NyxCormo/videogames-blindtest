package fr.insalan.blindtest.khinsider;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

// Lit les pages KHInsider : lien audio direct d'une musique, pochettes d'une page d'album.
@Component
public class KhinsiderClient {

    public static final String ALBUM_PAGES = "https://downloads.khinsider.com/game-soundtracks/album/";
    public static final String USER_AGENT = "blindtest-insalan (French Student Association, https://github.com/NyxCormo/videogames-blindtest)";
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

    public static String albumUrlOf(String songUrl) {
        return songUrl.substring(0, songUrl.lastIndexOf('/'));
    }

    public static List<String> extractAlbumImages(Document page, String albumSlug) {
        Pattern image = Pattern.compile(
                "https://([a-z0-9-]+\\.)?vgmtreasurechest\\.com/soundtracks/" + Pattern.quote(albumSlug) + "/[^/]+\\.(jpe?g|png|gif|webp)",
                Pattern.CASE_INSENSITIVE);
        return page.select("a[href]").stream()
                .map(link -> link.attr("abs:href"))
                .filter(url -> image.matcher(url).matches())
                .distinct()
                .toList();
    }
}
