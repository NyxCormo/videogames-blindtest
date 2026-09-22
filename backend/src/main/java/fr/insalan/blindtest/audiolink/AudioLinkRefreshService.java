package fr.insalan.blindtest.audiolink;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import fr.insalan.blindtest.khinsider.KhinsiderClient;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.repository.TrackRepository;


/*
 * Garde audio_link à jour pour les musiques qui ont une page KHInsider. 
 * Ne resollicite KHInsider que pour les liens morts ou jamais résolus.
 */
@Service 
public class AudioLinkRefreshService {
 
    private static final Logger log = LoggerFactory.getLogger(AudioLinkRefreshService.class);

    private final TrackRepository trackRepository;
    private final AudioLinkChecker audioLinkChecker;
    private final KhinsiderClient khinsiderClient;

    public AudioLinkRefreshService(TrackRepository trackRepository, AudioLinkChecker audioLinkChecker, KhinsiderClient khinsiderClient) {
        this.trackRepository = trackRepository;
        this.audioLinkChecker = audioLinkChecker;
        this.khinsiderClient = khinsiderClient;
    }

    public AudioLinkRefreshReport refresh() {
        List<Track> eligible = trackRepository.findByKhinsiderLinkIsNotNull();
        int alive = 0;
        int refreshed = 0;
        int failed = 0;

        for (Track track : eligible) {
            try {
                if (refreshOne(track)) {
                    refreshed++;
                } else {
                    alive++;
                }
            } catch (IOException e) {
                failed++;
                log.warn("Impossible de rafraîchir le lien audio de la musique {} : {}", track.getId(), e.getMessage());
            }
        }

        return new AudioLinkRefreshReport(eligible.size(), alive, refreshed, failed);
    }

    private boolean refreshOne(Track track) throws IOException {
        if (track.getAudioLink() != null && audioLinkChecker.isAlive(track.getAudioLink())) {
            return false;
        }

        String audioLink = khinsiderClient.resolveAudioLink(track.getKhinsiderLink()).orElse(null);
        track.setAudioLink(audioLink);
        track.setAudioLinkResolvedAt(audioLink != null ? Instant.now() : null);
        trackRepository.save(track);
        return true;
    }
}
