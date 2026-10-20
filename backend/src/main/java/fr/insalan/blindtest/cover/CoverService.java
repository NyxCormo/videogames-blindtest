package fr.insalan.blindtest.cover;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;

import fr.insalan.blindtest.khinsider.KhinsiderClient;
import fr.insalan.blindtest.model.AlbumScan;
import fr.insalan.blindtest.model.Game;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.repository.AlbumScanRepository;
import fr.insalan.blindtest.repository.CoverProposalRepository;
import fr.insalan.blindtest.repository.GameRepository;
import fr.insalan.blindtest.repository.TrackRepository;

@Service
public class CoverService {

    private record Album(String pageUrl, List<String> tracks) {}

    private final GameRepository gameRepository;
    private final TrackRepository trackRepository;
    private final CoverProposalRepository coverProposalRepository;
    private final AlbumScanRepository albumScanRepository;
    private final CoverFinder coverFinder;
    private final CoverStore coverStore;

    public CoverService(
        GameRepository gameRepository,
        TrackRepository trackRepository,
        CoverProposalRepository coverProposalRepository,
        AlbumScanRepository albumScanRepository,
        CoverFinder coverFinder,
        CoverStore coverStore
    ) {
        this.gameRepository = gameRepository;
        this.trackRepository = trackRepository;
        this.coverProposalRepository = coverProposalRepository;
        this.albumScanRepository = albumScanRepository;
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

    public List<AlbumImages> candidates(Integer gameId, boolean rescan) {
        Map<String, Album> albums = albumsByGame(trackRepository.findByGameIdAndAudioLinkIsNotNullOrderById(gameId))
            .getOrDefault(gameId, Map.of());
        return albums.entrySet().stream()
            .map(album -> {
                AlbumScan scan = rescan ? null : albumScanRepository.findById(album.getKey()).orElse(null);
                List<String> images = (scan != null ? scan : scan(album.getKey())).getImages();
                return new AlbumImages(album.getValue().pageUrl(), album.getValue().tracks(), images);
            })
            .toList();
    }

    public CoverFillReport fill(CoverFillMode mode) {
        Map<Integer, Map<String, Album>> albumsByGame = albumsByGame(trackRepository.findWithGameByAudioLinkIsNotNull());
        Map<String, AlbumScan> scans = new HashMap<>();
        albumScanRepository.findAllWithImages().forEach(scan -> scans.put(scan.getAlbumUrl(), scan));

        int scanned = 0;
        for (String album : albumsByGame.values().stream().flatMap(albums -> albums.keySet().stream()).distinct().toList()) {
            if (mode.needsScan(scans.get(album))) {
                scans.put(album, scan(album));
                scanned++;
            }
        }

        int covered = 0;
        int downloaded = 0;
        int notDownloaded = 0;
        for (Game game : gameRepository.findAll()) {
            if (game.getCoverUrl() == null) {
                Optional<String> image = albumsByGame.getOrDefault(game.getId(), Map.of()).keySet().stream()
                    .map(scans::get)
                    .filter(Objects::nonNull)
                    .flatMap(scan -> scan.getImages().stream())
                    .findFirst();
                if (image.isPresent()) {
                    setCover(game, image.get());
                    covered++;
                }
            } else if (coverStore.find(game.getId()).isEmpty()) {
                coverStore.download(game.getId(), game.getCoverUrl());
            }
            if (game.getCoverUrl() != null) {
                if (coverStore.find(game.getId()).isPresent()) {
                    downloaded++;
                } else {
                    notDownloaded++;
                }
            }
        }
        return new CoverFillReport(scanned, covered, downloaded, notDownloaded);
    }

    private AlbumScan scan(String albumUrl) {
        AlbumScan scan = albumScanRepository.findById(albumUrl).orElseGet(() -> new AlbumScan(albumUrl));
        scan.record(coverFinder.findAll(albumUrl));
        return albumScanRepository.save(scan);
    }

    private static Map<Integer, Map<String, Album>> albumsByGame(List<Track> tracks) {
        Map<Integer, Map<String, Album>> albumsByGame = new HashMap<>();
        for (Track track : tracks) {
            CoverFinder.albumOf(track.getAudioLink()).ifPresent(album -> albumsByGame
                .computeIfAbsent(track.getGame().getId(), id -> new LinkedHashMap<>())
                .computeIfAbsent(album, key -> new Album(
                    track.getKhinsiderLink() != null ? KhinsiderClient.albumUrlOf(track.getKhinsiderLink()) : key,
                    new ArrayList<>()))
                .tracks().add(track.getName()));
        }
        return albumsByGame;
    }
}
