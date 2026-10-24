package fr.insalan.blindtest.youtube;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import fr.insalan.blindtest.khinsider.RequestPacer;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.repository.TrackRepository;

@Service
public class YoutubeAudioService {

    private static final Logger log = LoggerFactory.getLogger(YoutubeAudioService.class);
    private static final int MAX_ERROR_LENGTH = 500;

    private final TrackRepository trackRepository;
    private final YoutubeAudioStore store;
    private final ObjectProvider<AudioDownloader> downloaders;
    private final RequestPacer pacer;
    private final boolean enabled;

    public YoutubeAudioService(
        TrackRepository trackRepository,
        YoutubeAudioStore store,
        ObjectProvider<AudioDownloader> downloaders,
        RequestPacer pacer,
        @Value("${youtube.downloads-enabled:true}") boolean enabled
    ) {
        this.trackRepository = trackRepository;
        this.store = store;
        this.downloaders = downloaders;
        this.pacer = pacer;
        this.enabled = enabled;
    }

    public static String localLink(Integer trackId) {
        return "/api/tracks/" + trackId + "/audio";
    }

    public static boolean isLocalLink(String audioLink) {
        return audioLink != null && audioLink.startsWith("/api/tracks/");
    }

    public boolean available() {
        return enabled && downloaders.getIfAvailable() != null;
    }

    public boolean needsYoutube(Track track) {
        return track.getYoutubeLink() != null
            && (track.getKhinsiderLink() == null || track.getAudioLink() == null || isLocalLink(track.getAudioLink()));
    }

    public boolean hasFile(Track track) {
        return store.find(track.getYoutubeAudioFile()).isPresent();
    }

    public YoutubeAudioReport downloadMissing(Consumer<YoutubeAudioReport> progress) {
        List<Track> tracks = trackRepository.findByYoutubeLinkIsNotNull();
        for (Track track : tracks) {
            useFileIfNeeded(track);
        }
        List<Integer> missing = available()
            ? tracks.stream().filter(track -> needsYoutube(track) && !hasFile(track)).map(Track::getId).toList()
            : List.of();
        int done = 0;
        int downloaded = 0;
        int failed = 0;
        for (Integer trackId : missing) {
            Track track = trackRepository.findById(trackId).orElse(null);
            if (track != null && needsYoutube(track) && !hasFile(track)) {
                if (download(track)) {
                    downloaded++;
                } else {
                    failed++;
                }
            }
            done++;
            progress.accept(new YoutubeAudioReport(done, missing.size(), downloaded, failed));
        }
        return new YoutubeAudioReport(done, missing.size(), downloaded, failed);
    }

    public boolean download(Track track) {
        AudioDownloader downloader = enabled ? downloaders.getIfAvailable() : null;
        String link = track.getYoutubeLink();
        if (downloader == null || link == null) {
            return false;
        }
        Path temporary = null;
        try {
            pacer.waitForTurn();
            temporary = store.newDownloadDirectory();
            Path file = downloader.download(link, temporary);
            Track current = withSameLink(track.getId(), link);
            if (current == null) {
                return false;
            }
            current.setYoutubeAudioFile(store.keep(current.getId(), file));
            current.setYoutubeAudioDownloadedAt(Instant.now());
            current.setYoutubeAudioError(null);
            if (current.getAudioLink() == null) {
                current.setAudioLink(localLink(current.getId()));
            }
            trackRepository.save(current);
            return true;
        } catch (IOException | RuntimeException e) {
            log.warn("Impossible de télécharger l'audio YouTube de la musique {} : {}", track.getId(), e.getMessage());
            Track current = withSameLink(track.getId(), link);
            if (current != null) {
                String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                current.setYoutubeAudioError(message.length() > MAX_ERROR_LENGTH ? message.substring(0, MAX_ERROR_LENGTH) : message);
                trackRepository.save(current);
            }
            return false;
        } finally {
            try {
                store.deleteDirectory(temporary);
            } catch (IOException e) {
                log.warn("Dossier temporaire non supprimé : {}", e.getMessage());
            }
        }
    }

    private Track withSameLink(Integer trackId, String youtubeLink) {
        return trackRepository.findById(trackId)
            .filter(current -> youtubeLink.equals(current.getYoutubeLink()))
            .orElse(null);
    }

    public void useFileIfNeeded(Track track) {
        boolean file = hasFile(track);
        if (track.getAudioLink() == null && file) {
            track.setAudioLink(localLink(track.getId()));
            trackRepository.save(track);
        } else if (isLocalLink(track.getAudioLink()) && !file) {
            track.setAudioLink(null);
            trackRepository.save(track);
        }
    }

    public void forget(Track track) {
        try {
            store.delete(track.getId());
        } catch (IOException e) {
            log.warn("Audio YouTube de la musique {} non supprimé : {}", track.getId(), e.getMessage());
        }
        track.setYoutubeAudioFile(null);
        track.setYoutubeAudioDownloadedAt(null);
        track.setYoutubeAudioError(null);
        if (isLocalLink(track.getAudioLink())) {
            track.setAudioLink(null);
        }
    }

    public YoutubeAudioStatus status(boolean running, Instant startedAt, Instant finishedAt, YoutubeAudioReport lastReport) {
        List<Track> tracks = trackRepository.findWithGameByYoutubeLinkIsNotNull().stream().filter(this::needsYoutube).toList();
        List<YoutubeAudioFailure> failures = tracks.stream()
            .filter(track -> track.getYoutubeAudioError() != null && !hasFile(track))
            .map(track -> new YoutubeAudioFailure(
                track.getId(),
                track.getName(),
                track.getGame().getName(),
                track.getYoutubeLink(),
                track.getYoutubeAudioError()))
            .toList();
        return new YoutubeAudioStatus(available(), running, startedAt, finishedAt, lastReport,
            tracks.size(), tracks.stream().filter(this::hasFile).count(), failures);
    }
}
