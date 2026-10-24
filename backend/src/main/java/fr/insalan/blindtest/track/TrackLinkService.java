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
import fr.insalan.blindtest.repository.TrackReportRepository;
import fr.insalan.blindtest.repository.TrackRepository;
import fr.insalan.blindtest.youtube.YoutubeAudioJob;
import fr.insalan.blindtest.youtube.YoutubeAudioService;

@Service
public class TrackLinkService {

    private static final Logger log = LoggerFactory.getLogger(TrackLinkService.class);

    private final TrackRepository trackRepository;
    private final AudioLinkRefreshService audioLinkRefreshService;
    private final TrackReportRepository trackReportRepository;
    private final YoutubeAudioService youtubeAudioService;
    private final YoutubeAudioJob youtubeAudioJob;

    public TrackLinkService(
        TrackRepository trackRepository,
        AudioLinkRefreshService audioLinkRefreshService,
        TrackReportRepository trackReportRepository,
        YoutubeAudioService youtubeAudioService,
        YoutubeAudioJob youtubeAudioJob
    ) {
        this.trackRepository = trackRepository;
        this.audioLinkRefreshService = audioLinkRefreshService;
        this.trackReportRepository = trackReportRepository;
        this.youtubeAudioService = youtubeAudioService;
        this.youtubeAudioJob = youtubeAudioJob;
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
        trackReportRepository.deleteByTrackId(track.getId());
        try {
            audioLinkRefreshService.refreshOne(track);
        } catch (IOException e) {
            log.warn("Impossible de résoudre le lien audio pour la musique {} : {}", track.getId(), e.getMessage());
        }
        youtubeAudioService.useFileIfNeeded(track);
        downloadYoutubeIfNeeded(track);
    }

    public void removeKhinsiderLink(Track track) {
        track.setKhinsiderLink(null);
        track.setAudioLink(null);
        track.setAudioLinkResolvedAt(null);
        trackRepository.save(track);
        trackReportRepository.deleteByTrackId(track.getId());
        youtubeAudioService.useFileIfNeeded(track);
        downloadYoutubeIfNeeded(track);
    }

    public void setYoutubeLink(Track track, String link) {
        requireHttpLink(link);
        if (!link.contains("youtube.com") && !link.contains("youtu.be")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le lien doit pointer vers youtube.com ou youtu.be");
        }
        requireUnused(trackRepository.findFirstByYoutubeLink(link), track);
        if (!link.equals(track.getYoutubeLink())) {
            youtubeAudioService.forget(track);
        }
        track.setYoutubeLink(link);
        trackRepository.save(track);
        downloadYoutubeIfNeeded(track);
    }

    public void removeYoutubeLink(Track track) {
        youtubeAudioService.forget(track);
        track.setYoutubeLink(null);
        trackRepository.save(track);
    }

    private void downloadYoutubeIfNeeded(Track track) {
        if (youtubeAudioService.needsYoutube(track) && !youtubeAudioService.hasFile(track)) {
            youtubeAudioJob.requestRun();
        }
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
