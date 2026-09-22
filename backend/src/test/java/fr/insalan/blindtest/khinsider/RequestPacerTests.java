package fr.insalan.blindtest.khinsider;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

// Pas de contexte Spring ici : RequestPacer se construit et se teste seul, sans serveur ni base.
class RequestPacerTests {

    @Test
    void doesNotWaitOnTheFirstCall() {
        RequestPacer pacer = new RequestPacer(10_000); // délai volontairement long

        Instant start = Instant.now();
        pacer.waitForTurn();
        Duration elapsed = Duration.between(start, Instant.now());

        assertTrue(elapsed.toMillis() < 1000, "le premier appel ne doit pas attendre : " + elapsed.toMillis() + " ms");
    }

    @Test
    void waitsAtLeastTheConfiguredDelayBetweenTwoCalls() {
        RequestPacer pacer = new RequestPacer(200);
        pacer.waitForTurn(); // premier appel : établit le point de départ, n'attend pas

        Instant start = Instant.now();
        pacer.waitForTurn();
        Duration elapsed = Duration.between(start, Instant.now());

        // Thread.sleep n'est pas garanti précis à la milliseconde près : petite tolérance
        assertTrue(elapsed.toMillis() >= 190, "attendu environ 200 ms, mesuré " + elapsed.toMillis() + " ms");
    }
}
