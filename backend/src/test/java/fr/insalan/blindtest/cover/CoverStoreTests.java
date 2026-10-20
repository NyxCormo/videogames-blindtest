package fr.insalan.blindtest.cover;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sun.net.httpserver.HttpServer;

import fr.insalan.blindtest.khinsider.RequestPacer;

class CoverStoreTests {

    private static final byte[] JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00 };
    private static final byte[] PNG = { (byte) 0x89, 'P', 'N', 'G' };

    @TempDir
    Path directory;

    private HttpServer server;
    private CoverStore store;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/front.jpg", exchange -> respond(exchange, "image/jpeg", JPEG));
        server.createContext("/front.png", exchange -> respond(exchange, "image/png", PNG));
        server.createContext("/page.html", exchange -> respond(exchange, "text/html", "<html></html>".getBytes()));
        server.start();
        store = new CoverStore(directory.toString(), true, new RequestPacer(0));
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void downloadsAnImageUnderTheIdOfTheGame() throws IOException {
        assertTrue(store.download(7, url("/front.jpg")));

        Optional<Path> file = store.find(7);
        assertEquals(Optional.of(directory.resolve("7.jpg")), file);
        assertArrayEquals(JPEG, Files.readAllBytes(file.get()));
    }

    @Test
    void refusesWhatIsNotAnImage() {
        assertFalse(store.download(7, url("/page.html")));
        assertFalse(store.download(7, url("/missing.jpg")));

        assertEquals(Optional.empty(), store.find(7));
    }

    @Test
    void aNewImageReplacesTheOldOneAndCanBeDeleted() {
        store.download(7, url("/front.png"));
        store.download(7, url("/front.jpg"));

        assertEquals(Optional.of(directory.resolve("7.jpg")), store.find(7));
        assertFalse(Files.exists(directory.resolve("7.png")));

        store.delete(7);
        assertEquals(Optional.empty(), store.find(7));
    }

    @Test
    void downloadsNothingWhenDisabled() {
        CoverStore disabled = new CoverStore(directory.toString(), false, new RequestPacer(0));

        assertFalse(disabled.download(7, url("/front.jpg")));
        assertEquals(Optional.empty(), disabled.find(7));
    }

    private String url(String path) {
        return "http://localhost:" + server.getAddress().getPort() + path;
    }

    private void respond(com.sun.net.httpserver.HttpExchange exchange, String type, byte[] body) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", type);
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}
