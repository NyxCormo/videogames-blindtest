package fr.insalan.blindtest.audiolink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


/*
 * Déclencheur planifié 
 */
@Component 
public class AudioLinkRefreshScheduler {
    
    private static final Logger log = LoggerFactory.getLogger(AudioLinkRefreshScheduler.class);

    private final AudioLinkRefreshService audioLinkRefreshService;

    public AudioLinkRefreshScheduler(AudioLinkRefreshService audioLinkRefreshService) {
        this.audioLinkRefreshService = audioLinkRefreshService;
    }

    @Scheduled(fixedDelayString = "${khinsider.refresh-interval:7d}")
    public void refresh() {
        AudioLinkRefreshReport report = audioLinkRefreshService.refresh();
        log.info("Rafraîchissement des liens audio : {} musiques avec une page KHInsider, {} déjà vivantes, "
            + "{} rafraîchies, {} en échec",
            report.checked(), 
            report.alive(), 
            report.refreshed(), 
            report.failed()
        );
    }
}
