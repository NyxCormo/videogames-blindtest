package fr.insalan.blindtest.youtube;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class YoutubeAudioJob {

    private static final Logger log = LoggerFactory.getLogger(YoutubeAudioJob.class);

    private final YoutubeAudioService youtubeAudioService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean runAgain = new AtomicBoolean(false);
    private volatile Instant startedAt;
    private volatile Instant finishedAt;
    private volatile YoutubeAudioReport lastReport;

    public YoutubeAudioJob(YoutubeAudioService youtubeAudioService) {
        this.youtubeAudioService = youtubeAudioService;
    }

    public boolean startInBackground() {
        if (!begin()) {
            return false;
        }
        new Thread(this::runAndFinish, "audios-youtube").start();
        return true;
    }

    public void requestRun() {
        if (!youtubeAudioService.available()) {
            return;
        }
        if (!startInBackground()) {
            runAgain.set(true);
        }
    }

    public void runIfIdle() {
        if (begin()) {
            runAndFinish();
        }
    }

    public YoutubeAudioStatus status() {
        return youtubeAudioService.status(running.get(), startedAt, finishedAt, lastReport);
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
            do {
                runAgain.set(false);
                lastReport = null;
                lastReport = youtubeAudioService.downloadMissing(report -> lastReport = report);
                log.info("Audios YouTube : {} téléchargés, {} en échec", lastReport.downloaded(), lastReport.failed());
            } while (runAgain.get());
        } catch (RuntimeException e) {
            log.error("Le téléchargement des audios YouTube a échoué", e);
        } finally {
            finishedAt = Instant.now();
            running.set(false);
        }
    }
}
