package fr.insalan.blindtest.audiolink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import fr.insalan.blindtest.khinsider.RequestPacer;

// Un vrai petit serveur HTTP local (fourni par le JDK) plutôt qu'une bibliothèque de simulation :
// on vérifie le comportement réel d'AudioLinkChecker face à de vraies réponses HTTP.
class AudioLinkCheckerTests {

    private HttpServer server;
    private String requestedMethod;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/alive", exchange -> respond(exchange, 200));
        server.createContext("/gone", exchange -> respond(exchange, 404));
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private void respond(HttpExchange exchange, int status) throws IOException {
        requestedMethod = exchange.getRequestMethod();
        exchange.sendResponseHeaders(status, -1);
        exchange.close();
    }

    private String urlFor(String path) {
        return "http://localhost:" + server.getAddress().getPort() + path;
    }

    @Test
    void returnsTrueWhenTheLinkAnswers200() {
        AudioLinkChecker checker = new AudioLinkChecker(new RequestPacer(0));

        assertTrue(checker.isAlive(urlFor("/alive")));
    }

    @Test
    void returnsFalseWhenTheLinkAnswers404() {
        AudioLinkChecker checker = new AudioLinkChecker(new RequestPacer(0));

        assertFalse(checker.isAlive(urlFor("/gone")));
    }

    @Test
    void returnsFalseWhenTheHostIsUnreachable() {
        AudioLinkChecker checker = new AudioLinkChecker(new RequestPacer(0));

        assertFalse(checker.isAlive("http://localhost:1/introuvable"));
    }

    @Test
    void sendsAHeadRequestRatherThanAGet() {
        AudioLinkChecker checker = new AudioLinkChecker(new RequestPacer(0));

        checker.isAlive(urlFor("/alive"));

        assertEquals("HEAD", requestedMethod);
    }
}
