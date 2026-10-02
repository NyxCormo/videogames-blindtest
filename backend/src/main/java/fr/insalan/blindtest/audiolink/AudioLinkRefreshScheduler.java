package fr.insalan.blindtest.audiolink;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


/*
 * Déclencheur planifié 
 */
@Component 
public class AudioLinkRefreshScheduler {
    
    private final AudioLinkRefreshJob audioLinkRefreshJob;

    public AudioLinkRefreshScheduler(AudioLinkRefreshJob audioLinkRefreshJob) {
        this.audioLinkRefreshJob = audioLinkRefreshJob;
    }

    @Scheduled(
        initialDelayString = "${khinsider.refresh-initial-delay:1h}",
        fixedDelayString = "${khinsider.refresh-interval:7d}"
    )
    public void refresh() {
        audioLinkRefreshJob.runIfIdle();
    }
}
