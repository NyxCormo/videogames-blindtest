package fr.insalan.blindtest.audiolink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.sun.net.httpserver.HttpServer;

import fr.insalan.blindtest.model.Franchise;
import fr.insalan.blindtest.model.Game;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.repository.FranchiseRepository;
import fr.insalan.blindtest.repository.GameRepository;
import fr.insalan.blindtest.repository.TrackRepository;

// Pas de @Transactional : on veut vérifier que l'échec d'une musique n'annule pas ce qui a
// déjà été enregistré pour les autres (voir ImportTransactionTests pour la logique inverse).
@SpringBootTest
@ActiveProfiles("test")
class AudioLinkRefreshServiceTests {

    @Autowired
    AudioLinkRefreshService refreshService;

    @Autowired
    FranchiseRepository franchises;

    @Autowired
    GameRepository games;

    @Autowired
    TrackRepository tracks;

    private HttpServer server;
    private AtomicInteger khinsiderHits;
    private Game stellarBlade;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        khinsiderHits = new AtomicInteger();

        server.createContext("/audio/alive", exchange -> respondWith(exchange, 200, ""));
        server.createContext("/audio/dead", exchange -> respondWith(exchange, 404, ""));
        server.createContext("/khinsider/with-link", exchange -> {
            khinsiderHits.incrementAndGet();
            respondWith(exchange, 200, "<html><body><audio id=\"audio\" src=\"" + url("/audio/alive") + "\"></audio></body></html>");
        });
        server.createContext("/khinsider/no-link", exchange -> {
            khinsiderHits.incrementAndGet();
            respondWith(exchange, 200, "<html><body>Musique indisponible</body></html>");
        });
        server.start();

        Franchise franchise = franchises.save(new Franchise("Stellar Blade"));
        stellarBlade = games.save(new Game("Stellar Blade", franchise));
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
        tracks.deleteAll();
        games.deleteAll();
        franchises.deleteAll();
    }

    private String url(String path) {
        return "http://localhost:" + server.getAddress().getPort() + path;
    }

    private void respondWith(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, exchange.getRequestMethod().equals("HEAD") ? -1 : bytes.length);
        if (!exchange.getRequestMethod().equals("HEAD")) {
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }

    @Test
    void refreshesADeadLinkFromItsKhinsiderPage() {
        Track dawn = new Track("Dawn", stellarBlade);
        dawn.setKhinsiderLink(url("/khinsider/with-link"));
        dawn.setAudioLink(url("/audio/dead"));
        tracks.save(dawn);

        AudioLinkRefreshReport report = refreshService.refresh();

        assertEquals(new AudioLinkRefreshReport(1, 0, 1, 0), report);
        Track updated = tracks.findById(dawn.getId()).orElseThrow();
        assertEquals(url("/audio/alive"), updated.getAudioLink());
    }

    @Test
    void doesNotRescrapeAnAliveLink() {
        Track dawn = new Track("Dawn", stellarBlade);
        dawn.setKhinsiderLink(url("/khinsider/with-link"));
        dawn.setAudioLink(url("/audio/alive"));
        tracks.save(dawn);

        AudioLinkRefreshReport report = refreshService.refresh();

        assertEquals(new AudioLinkRefreshReport(1, 1, 0, 0), report);
        assertEquals(0, khinsiderHits.get(), "la page KHInsider n'aurait pas dû être re-sollicitée");
    }

    @Test
    void clearsTheLinkWhenThePageNoLongerHasOne() {
        Track dawn = new Track("Dawn", stellarBlade);
        dawn.setKhinsiderLink(url("/khinsider/no-link"));
        dawn.setAudioLink(url("/audio/dead"));
        tracks.save(dawn);

        refreshService.refresh();

        Track updated = tracks.findById(dawn.getId()).orElseThrow();
        assertNull(updated.getAudioLink());
        assertNull(updated.getAudioLinkResolvedAt());
    }

    @Test
    void oneFailureDoesNotPreventTheOthersFromBeingSaved() {
        Track dawn = new Track("Dawn", stellarBlade);
        dawn.setKhinsiderLink(url("/khinsider/with-link"));
        dawn.setAudioLink(url("/audio/dead"));
        tracks.save(dawn);

        Track raven = new Track("Raven", stellarBlade);
        raven.setKhinsiderLink("http://localhost:1/introuvable"); // hôte injoignable : échoue
        tracks.save(raven);

        AudioLinkRefreshReport report = refreshService.refresh();

        assertEquals(1, report.failed());
        assertEquals(1, report.refreshed());
        Track updatedDawn = tracks.findById(dawn.getId()).orElseThrow();
        assertEquals(url("/audio/alive"), updatedDawn.getAudioLink());
    }
}
