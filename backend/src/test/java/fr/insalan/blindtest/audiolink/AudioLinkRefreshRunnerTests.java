package fr.insalan.blindtest.audiolink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import fr.insalan.blindtest.model.Franchise;
import fr.insalan.blindtest.model.Game;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.repository.FranchiseRepository;
import fr.insalan.blindtest.repository.GameRepository;
import fr.insalan.blindtest.repository.TrackRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AudioLinkRefreshRunnerTests {

    @Autowired
    AudioLinkRefreshRunner refreshRunner;

    @Autowired
    FranchiseRepository franchises;

    @Autowired
    GameRepository games;

    @Autowired
    TrackRepository tracks;

    private HttpServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/audio/alive", exchange -> respond(exchange, 200, ""));
        server.createContext("/khinsider/dawn", exchange -> respond(exchange, 200,
                "<html><body><audio id=\"audio\" src=\"" + url("/audio/alive") + "\"></audio></body></html>"));
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private String url(String path) {
        return "http://localhost:" + server.getAddress().getPort() + path;
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, exchange.getRequestMethod().equals("HEAD") ? -1 : bytes.length);
        if (!exchange.getRequestMethod().equals("HEAD")) {
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }

    private Track createTrackWithKhinsiderLink() {
        Franchise franchise = franchises.save(new Franchise("Stellar Blade"));
        Game game = games.save(new Game("Stellar Blade", franchise));
        Track dawn = new Track("Dawn", game);
        dawn.setKhinsiderLink(url("/khinsider/dawn"));
        return tracks.save(dawn);
    }

    @Test
    void refreshesLinksWhenTheOptionIsPresent() {
        Track dawn = createTrackWithKhinsiderLink();

        refreshRunner.run(new DefaultApplicationArguments("--refresh-links"));

        Track updated = tracks.findById(dawn.getId()).orElseThrow();
        assertEquals(url("/audio/alive"), updated.getAudioLink());
    }

    @Test
    void doesNothingWithoutTheOption() {
        Track dawn = createTrackWithKhinsiderLink();

        refreshRunner.run(new DefaultApplicationArguments());

        Track unchanged = tracks.findById(dawn.getId()).orElseThrow();
        assertNull(unchanged.getAudioLink());
    }
}
