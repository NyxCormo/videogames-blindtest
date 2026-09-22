package fr.insalan.blindtest.khinsider;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


/*
 * Impose un délai entre 2 requêtes vers un hôte externe. Un seul RequestPacer partagé entre tous les hôtes implique 
 * que le délai n'est pas raccourci par la présence de plusieurs services faisant des requêtes
 */
@Component
public class RequestPacer {

    private final Duration minDelay;
    private Instant lastCallAt = Instant.EPOCH;

    public RequestPacer(@Value("${khinsider.request-delay-ms:1000}") long minDelayMs) {
        this.minDelay = Duration.ofMillis(minDelayMs);
    }

    // Si deux threads appellent en même temps, l'un attend l'autre plutôt que de passer les deux au même instant.
    public synchronized void waitForTurn() {
        Duration elapsed = Duration.between(lastCallAt, Instant.now());
        Duration remaining = minDelay.minus(elapsed);
        if (!remaining.isNegative()) {
            sleep(remaining);
        }
        lastCallAt = Instant.now();
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Attente interrompue", e);
        }
    }
}
