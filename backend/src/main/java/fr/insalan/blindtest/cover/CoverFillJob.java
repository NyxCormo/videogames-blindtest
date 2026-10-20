package fr.insalan.blindtest.cover;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CoverFillJob {

    private static final Logger log = LoggerFactory.getLogger(CoverFillJob.class);

    private final CoverService coverService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile Instant startedAt;
    private volatile Instant finishedAt;
    private volatile CoverFillReport lastReport;

    public CoverFillJob(CoverService coverService) {
        this.coverService = coverService;
    }

    public boolean startInBackground() {
        if (!running.compareAndSet(false, true)) {
            return false;
        }
        startedAt = Instant.now();
        finishedAt = null;
        new Thread(this::runAndFinish, "images-des-jeux").start();
        return true;
    }

    public CoverFillStatus status() {
        return new CoverFillStatus(running.get(), startedAt, finishedAt, lastReport);
    }

    private void runAndFinish() {
        try {
            lastReport = coverService.fillMissing();
            log.info("Images des jeux : {} jeux traités, {} images trouvées, {} téléchargées, {} en échec",
                lastReport.checked(), lastReport.found(), lastReport.downloaded(), lastReport.failed());
        } catch (RuntimeException e) {
            log.error("La recherche des images des jeux a échoué", e);
        } finally {
            finishedAt = Instant.now();
            running.set(false);
        }
    }
}
