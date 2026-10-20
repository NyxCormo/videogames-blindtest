package fr.insalan.blindtest.cover;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import fr.insalan.blindtest.repository.AlbumScanRepository;

@Component
public class CoverFillJob {

    private static final Logger log = LoggerFactory.getLogger(CoverFillJob.class);

    private final CoverService coverService;
    private final AlbumScanRepository albumScanRepository;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile Instant startedAt;
    private volatile Instant finishedAt;
    private volatile CoverFillReport lastReport;

    public CoverFillJob(CoverService coverService, AlbumScanRepository albumScanRepository) {
        this.coverService = coverService;
        this.albumScanRepository = albumScanRepository;
    }

    public boolean startInBackground(CoverFillMode mode) {
        if (!running.compareAndSet(false, true)) {
            return false;
        }
        startedAt = Instant.now();
        finishedAt = null;
        new Thread(() -> runAndFinish(mode), "images-des-jeux").start();
        return true;
    }

    public CoverFillStatus status() {
        return new CoverFillStatus(running.get(), startedAt, finishedAt, lastReport,
            albumScanRepository.count(), albumScanRepository.countWithoutImage());
    }

    private void runAndFinish(CoverFillMode mode) {
        try {
            lastReport = null;
            lastReport = coverService.fill(mode, report -> lastReport = report);
            log.info("Images des jeux : {} albums recherchés, {} jeux ont reçu une image, {} images sur le serveur, {} non téléchargées",
                lastReport.scanned(), lastReport.covered(), lastReport.downloaded(), lastReport.notDownloaded());
        } catch (RuntimeException e) {
            log.error("La recherche des images des jeux a échoué", e);
        } finally {
            finishedAt = Instant.now();
            running.set(false);
        }
    }
}
