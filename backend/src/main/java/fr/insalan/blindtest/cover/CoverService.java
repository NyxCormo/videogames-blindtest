package fr.insalan.blindtest.cover;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import fr.insalan.blindtest.dto.GameResponse;
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
    private final AlbumPageStore albumPageStore;

    public CoverService(
        GameRepository gameRepository,
        TrackRepository trackRepository,
        CoverProposalRepository coverProposalRepository,
        AlbumScanRepository albumScanRepository,
        CoverFinder coverFinder,
        CoverStore coverStore,
        AlbumPageStore albumPageStore
    ) {
        this.gameRepository = gameRepository;
        this.trackRepository = trackRepository;
        this.coverProposalRepository = coverProposalRepository;
        this.albumScanRepository = albumScanRepository;
        this.coverFinder = coverFinder;
        this.coverStore = coverStore;
        this.albumPageStore = albumPageStore;
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
                Optional<List<String>> pageImages = pageImages(album.getKey());
                if (pageImages.isPresent()) {
                    return new AlbumImages(album.getValue().pageUrl(), album.getValue().tracks(), pageImages.get(), true);
                }
                AlbumScan scan = rescan ? null : albumScanRepository.findById(album.getKey()).orElse(null);
                List<String> images = (scan != null ? scan : scan(album.getKey())).getImages();
                return new AlbumImages(album.getValue().pageUrl(), album.getValue().tracks(), images, false);
            })
            .toList();
    }

    public CoverFillReport fill(CoverFillMode mode, Consumer<CoverFillReport> progress) {
        Map<Integer, Map<String, Album>> albumsByGame = albumsByGame(trackRepository.findWithGameByAudioLinkIsNotNull());
        Map<String, AlbumScan> scans = new HashMap<>();
        albumScanRepository.findAllWithImages().forEach(scan -> scans.put(scan.getAlbumUrl(), scan));
        Map<String, List<String>> images = new HashMap<>();
        albumsByGame.values().stream()
            .flatMap(albums -> albums.keySet().stream())
            .distinct()
            .forEach(album -> pageImages(album).ifPresent(found -> images.put(album, found)));
        Set<String> toScan = albumsByGame.values().stream()
            .flatMap(albums -> albums.keySet().stream())
            .filter(album -> !images.containsKey(album) && mode.needsScan(scans.get(album)))
            .collect(Collectors.toCollection(HashSet::new));
        scans.forEach((album, scan) -> images.putIfAbsent(album, scan.getImages()));
        int total = toScan.size();

        int scanned = 0;
        int covered = 0;
        int downloaded = 0;
        int notDownloaded = 0;
        for (Game game : gameRepository.findAll()) {
            Map<String, Album> albums = albumsByGame.getOrDefault(game.getId(), Map.of());
            for (String album : albums.keySet()) {
                if (toScan.remove(album)) {
                    images.put(album, scan(album).getImages());
                    scanned++;
                }
            }
            if (game.getCoverUrl() == null) {
                Optional<String> image = albums.keySet().stream()
                    .map(images::get)
                    .filter(Objects::nonNull)
                    .flatMap(List::stream)
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
            progress.accept(new CoverFillReport(scanned, total, covered, downloaded, notDownloaded));
        }
        return new CoverFillReport(scanned, total, covered, downloaded, notDownloaded);
    }

    public List<AlbumOverview> albums() {
        Map<String, AlbumScan> scans = new HashMap<>();
        albumScanRepository.findAllWithImages().forEach(scan -> scans.put(scan.getAlbumUrl(), scan));
        Map<String, List<Track>> tracksByAlbum = new LinkedHashMap<>();
        for (Track track : trackRepository.findWithGameByAudioLinkIsNotNull()) {
            CoverFinder.albumOf(track.getAudioLink())
                .ifPresent(album -> tracksByAlbum.computeIfAbsent(album, key -> new ArrayList<>()).add(track));
        }
        return tracksByAlbum.entrySet().stream()
            .map(album -> {
                List<Track> tracks = album.getValue();
                AlbumScan scan = scans.get(album.getKey());
                Optional<List<String>> pageImages = pageImages(album.getKey());
                String pageUrl = tracks.stream()
                    .filter(track -> track.getKhinsiderLink() != null)
                    .map(track -> KhinsiderClient.albumUrlOf(track.getKhinsiderLink()))
                    .findFirst()
                    .orElse(null);
                List<GameResponse> games = tracks.stream()
                    .map(Track::getGame)
                    .distinct()
                    .map(GameResponse::from)
                    .toList();
                return new AlbumOverview(
                    album.getKey(),
                    pageUrl,
                    slugOf(album.getKey()),
                    games,
                    tracks.stream().map(Track::getName).toList(),
                    scan == null ? null : scan.getScannedAt(),
                    albumPageStore.savedAt(slugOf(album.getKey())).orElse(null),
                    pageImages.orElse(scan == null ? List.of() : scan.getImages()));
            })
            .sorted(Comparator.comparing(AlbumOverview::name))
            .toList();
    }

    public Optional<AlbumImport> importAlbumPage(String pageUrl, String html) {
        String slug = slugOf(pageUrl);
        List<Track> tracks = trackRepository.findWithGameByAudioLinkIsNotNull().stream()
            .filter(track -> CoverFinder.albumOf(track.getAudioLink()).map(album -> slugOf(album).equals(slug)).orElse(false))
            .toList();
        if (tracks.isEmpty()) {
            return Optional.empty();
        }
        Document page = Jsoup.parse(html, pageUrl);
        albumPageStore.save(slug, albumImageBlocks(page, slug));
        List<String> images = KhinsiderClient.extractAlbumImages(page, slug);
        List<String> covered = new ArrayList<>();
        if (!images.isEmpty()) {
            tracks.stream()
                .map(Track::getGame)
                .distinct()
                .filter(game -> game.getCoverUrl() == null)
                .forEach(game -> {
                    setCover(game, images.get(0));
                    covered.add(game.getName());
                });
        }
        return Optional.of(new AlbumImport(slug, images, covered));
    }

    private static String albumImageBlocks(Document page, String slug) {
        Elements blocks = page.select("div.albumImage");
        if (!blocks.isEmpty()) {
            return blocks.outerHtml();
        }
        return KhinsiderClient.extractAlbumImages(page, slug).stream()
            .map(url -> "<a href=\"" + url + "\"></a>")
            .collect(Collectors.joining("\n"));
    }

    private Optional<List<String>> pageImages(String album) {
        String slug = slugOf(album);
        return albumPageStore.find(slug)
            .map(html -> KhinsiderClient.extractAlbumImages(Jsoup.parse(html, KhinsiderClient.ALBUM_PAGES + slug), slug));
    }

    private static String slugOf(String url) {
        String path = url.replaceAll("[?#].*$", "").replaceAll("/+$", "");
        return path.substring(path.lastIndexOf('/') + 1);
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
