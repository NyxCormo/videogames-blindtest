package fr.insalan.blindtest.audiolink;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// Un seul balayage à la fois, qu'il vienne de la tâche planifiée ou du bouton de la page admin.
@Component
public class AudioLinkRefreshJob {

    private static final Logger log = LoggerFactory.getLogger(AudioLinkRefreshJob.class);

    private final AudioLinkRefreshService audioLinkRefreshService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile Instant startedAt;
    private volatile Instant finishedAt;
    private volatile AudioLinkRefreshReport lastReport;

    public AudioLinkRefreshJob(AudioLinkRefreshService audioLinkRefreshService) {
        this.audioLinkRefreshService = audioLinkRefreshService;
    }

    // Renvoie false si un balayage tourne déjà.
    public boolean startInBackground() {
        if (!begin()) {
            return false;
        }
        new Thread(this::runAndFinish, "rafraichissement-liens").start();
        return true;
    }

    public void runIfIdle() {
        if (begin()) {
            runAndFinish();
        }
    }

    public AudioLinkRefreshStatus status() {
        return new AudioLinkRefreshStatus(running.get(), startedAt, finishedAt, lastReport);
    }

    private boolean begin() {
        if (!running.compareAndSet(false, true)) {
            return false;
        }
        startedAt = Instant.now();
        finishedAt = null;
        return true;
    }

    private void runAndFinish() {
        try {
            AudioLinkRefreshReport report = audioLinkRefreshService.refresh();
            lastReport = report;
            log.info("Rafraîchissement des liens audio : {} musiques avec une page KHInsider, {} déjà vivantes, "
                + "{} rafraîchies, {} en échec",
                report.checked(), report.alive(), report.refreshed(), report.failed());
        } catch (RuntimeException e) {
            log.error("Le rafraîchissement des liens audio a échoué", e);
        } finally {
            finishedAt = Instant.now();
            running.set(false);
        }
    }
}
