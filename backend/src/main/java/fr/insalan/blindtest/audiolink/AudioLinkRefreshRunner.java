package fr.insalan.blindtest.audiolink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/*
 * Lance un balayage des liens audio depuis la ligne de commande : --refresh-links
 * Sans cette option, l'application démarre normalement.
 */
@Component
public class AudioLinkRefreshRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(AudioLinkRefreshRunner.class);

    private final AudioLinkRefreshService refreshService;

    public AudioLinkRefreshRunner(AudioLinkRefreshService refreshService) {
        this.refreshService = refreshService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption("refresh-links")) {
            return;
        }

        AudioLinkRefreshReport report = refreshService.refresh();

        logger.info("Rafraîchissement terminé : {} musiques avec une page KHInsider, {} déjà vivantes, "
                + "{} rafraîchies, {} en échec.",
                report.checked(),
                report.alive(),
                report.refreshed(),
                report.failed()
        );
    }
}
