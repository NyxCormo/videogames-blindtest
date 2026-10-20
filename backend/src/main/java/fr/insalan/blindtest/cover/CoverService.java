package fr.insalan.blindtest.cover;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import fr.insalan.blindtest.khinsider.KhinsiderClient;
import fr.insalan.blindtest.model.Game;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.repository.CoverProposalRepository;
import fr.insalan.blindtest.repository.GameRepository;
import fr.insalan.blindtest.repository.TrackRepository;

@Service
public class CoverService {

    private record Album(String pageUrl, List<String> tracks) {}

    private final GameRepository gameRepository;
    private final TrackRepository trackRepository;
    private final CoverProposalRepository coverProposalRepository;
    private final CoverFinder coverFinder;
    private final CoverStore coverStore;

    public CoverService(
        GameRepository gameRepository,
        TrackRepository trackRepository,
        CoverProposalRepository coverProposalRepository,
        CoverFinder coverFinder,
        CoverStore coverStore
    ) {
        this.gameRepository = gameRepository;
        this.trackRepository = trackRepository;
        this.coverProposalRepository = coverProposalRepository;
        this.coverFinder = coverFinder;
        this.coverStore = coverStore;
    }

    public void setCover(Game game, String coverUrl) {
        coverStore.delete(game.getId());
        game.setCoverUrl(coverUrl);
        gameRepository.save(game);
        coverProposalRepository.deleteWithoutImageByGameId(game.getId());
        if (coverUrl != null) {
            coverStore.download(game.getId(), coverUrl);
        }
    }

    public List<AlbumImages> candidates(Integer gameId) {
        return albums(gameId).entrySet().stream()
            .map(album -> new AlbumImages(album.getValue().pageUrl(), album.getValue().tracks(), coverFinder.findAll(album.getKey())))
            .toList();
    }

    public CoverFillReport fillMissing() {
        int checked = 0;
        int found = 0;
        int downloaded = 0;
        int failed = 0;
        for (Game game : gameRepository.findAll()) {
            if (game.getCoverUrl() == null) {
                Map<String, Album> albums = albums(game.getId());
                if (albums.isEmpty()) {
                    continue;
                }
                checked++;
                Optional<String> image = coverFinder.findFirst(albums.keySet().iterator().next());
                if (image.isEmpty()) {
                    failed++;
                    continue;
                }
                setCover(game, image.get());
                found++;
                if (coverStore.find(game.getId()).isPresent()) {
                    downloaded++;
                }
            } else if (coverStore.find(game.getId()).isEmpty()) {
                checked++;
                if (coverStore.download(game.getId(), game.getCoverUrl())) {
                    downloaded++;
                } else {
                    failed++;
                }
            }
        }
        return new CoverFillReport(checked, found, downloaded, failed);
    }

    private Map<String, Album> albums(Integer gameId) {
        Map<String, Album> albums = new LinkedHashMap<>();
        for (Track track : trackRepository.findByGameIdAndAudioLinkIsNotNullOrderById(gameId)) {
            CoverFinder.albumOf(track.getAudioLink()).ifPresent(album -> albums
                .computeIfAbsent(album, key -> new Album(
                    track.getKhinsiderLink() != null ? KhinsiderClient.albumUrlOf(track.getKhinsiderLink()) : key,
                    new ArrayList<>()))
                .tracks().add(track.getName()));
        }
        return albums;
    }
}
