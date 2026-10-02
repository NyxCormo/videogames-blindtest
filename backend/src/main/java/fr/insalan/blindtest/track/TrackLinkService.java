package fr.insalan.blindtest.track;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import fr.insalan.blindtest.audiolink.AudioLinkRefreshService;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.repository.TrackRepository;

@Service
public class TrackLinkService {

    private static final Logger log = LoggerFactory.getLogger(TrackLinkService.class);

    private final TrackRepository trackRepository;
    private final AudioLinkRefreshService audioLinkRefreshService;

    public TrackLinkService(TrackRepository trackRepository, AudioLinkRefreshService audioLinkRefreshService) {
        this.trackRepository = trackRepository;
        this.audioLinkRefreshService = audioLinkRefreshService;
    }

    public void setKhinsiderLink(Track track, String link) {
        requireHttpLink(link);
        if (!link.contains("khinsider.com")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le lien doit pointer vers khinsider.com");
        }
        requireUnused(trackRepository.findFirstByKhinsiderLink(link), track);
        track.setKhinsiderLink(link);
        // Le lien audio venait de l'ancienne page : sans ça, refreshOne le garderait tant qu'il répond.
        track.setAudioLink(null);
        track.setAudioLinkResolvedAt(null);
        trackRepository.save(track);
        try {
            audioLinkRefreshService.refreshOne(track);
        } catch (IOException e) {
            log.warn("Impossible de résoudre le lien audio pour la musique {} : {}", track.getId(), e.getMessage());
        }
    }

    public void removeKhinsiderLink(Track track) {
        track.setKhinsiderLink(null);
        track.setAudioLink(null);
        track.setAudioLinkResolvedAt(null);
        trackRepository.save(track);
    }

    public void setYoutubeLink(Track track, String link) {
        requireHttpLink(link);
        if (!link.contains("youtube.com") && !link.contains("youtu.be")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le lien doit pointer vers youtube.com ou youtu.be");
        }
        requireUnused(trackRepository.findFirstByYoutubeLink(link), track);
        track.setYoutubeLink(link);
        trackRepository.save(track);
    }

    public void removeYoutubeLink(Track track) {
        track.setYoutubeLink(null);
        trackRepository.save(track);
    }

    // Un lien affiché tel quel dans un <a href> côté front : refuser tout ce qui n'est pas http(s)
    // évite qu'un lien "javascript:..." s'exécute au clic.
    private void requireHttpLink(String link) {
        if (link == null || link.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le lien est obligatoire");
        }
        if (!link.startsWith("http://") && !link.startsWith("https://")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le lien doit commencer par http:// ou https://");
        }
    }

    private void requireUnused(Optional<Track> other, Track track) {
        if (other.isPresent() && !other.get().getId().equals(track.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce lien est déjà utilisé par la musique « " + other.get().getName() + " »");
        }
    }
}
