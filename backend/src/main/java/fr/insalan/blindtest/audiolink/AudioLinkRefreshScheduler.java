package fr.insalan.blindtest.audiolink;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import fr.insalan.blindtest.youtube.YoutubeAudioJob;


/*
 * Déclencheur planifié 
 */
@Component 
public class AudioLinkRefreshScheduler {
    
    private final AudioLinkRefreshJob audioLinkRefreshJob;
    private final YoutubeAudioJob youtubeAudioJob;

    public AudioLinkRefreshScheduler(AudioLinkRefreshJob audioLinkRefreshJob, YoutubeAudioJob youtubeAudioJob) {
        this.audioLinkRefreshJob = audioLinkRefreshJob;
        this.youtubeAudioJob = youtubeAudioJob;
    }

    @Scheduled(
        initialDelayString = "${khinsider.refresh-initial-delay:1h}",
        fixedDelayString = "${khinsider.refresh-interval:7d}"
    )
    public void refresh() {
        audioLinkRefreshJob.runIfIdle();
        youtubeAudioJob.runIfIdle();
    }
}
