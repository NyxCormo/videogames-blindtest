package fr.insalan.blindtest.cover;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import fr.insalan.blindtest.khinsider.RequestPacer;

public class CoverFinderTests {

    private HttpServer server;
    private CoverFinder finder;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/soundtracks/", CoverFinderTests::fakeCdn);
        server.start();
        finder = new CoverFinder(new RequestPacer(0));
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    public static void fakeCdn(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getRawPath();
        if (path.equals("/soundtracks/stellar-blade-soundtrack-2024/00%20Front.jpg") || path.equals("/soundtracks/stellar-blade-soundtrack-2024/folder.jpg")) {
            exchange.getResponseHeaders().add("Content-Type", "image/jpeg");
            exchange.sendResponseHeaders(200, -1);
        } else if (path.endsWith("/Cover.jpg")) {
            exchange.getResponseHeaders().add("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, -1);
        } else {
            exchange.sendResponseHeaders(404, -1);
        }
        exchange.close();
    }

    @Test
    void theAlbumIsTheFolderAboveTheAudioFile() {
        assertEquals(
            Optional.of("https://jetta.vgmtreasurechest.com/soundtracks/stellar-blade-soundtrack-2024"),
            CoverFinder.albumOf("https://jetta.vgmtreasurechest.com/soundtracks/stellar-blade-soundtrack-2024/ybomulwy/62.%20Dawn.mp3"));
        assertEquals(Optional.empty(), CoverFinder.albumOf("https://example.org/dawn.mp3"));
    }

    @Test
    void findsTheImagesOfTheAlbumUnderTheirUsualNames() {
        String album = url("/soundtracks/stellar-blade-soundtrack-2024");

        assertEquals(List.of(album + "/00%20Front.jpg", album + "/folder.jpg"), finder.findAll(album));
        assertEquals(Optional.of(album + "/00%20Front.jpg"), finder.findFirst(album));
    }

    @Test
    void findsNothingWhenNoUsualNameIsAnImage() {
        String album = url("/soundtracks/stellar-blade-mini-original-soundtrack-2024");

        assertEquals(List.of(), finder.findAll(album));
        assertEquals(Optional.empty(), finder.findFirst(album));
    }

    private String url(String path) {
        return "http://localhost:" + server.getAddress().getPort() + path;
    }
}
