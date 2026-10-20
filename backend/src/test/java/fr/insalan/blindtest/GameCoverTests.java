package fr.insalan.blindtest;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.sun.net.httpserver.HttpServer;

import fr.insalan.blindtest.cover.AlbumPageStore;
import fr.insalan.blindtest.cover.CoverFillMode;
import fr.insalan.blindtest.cover.CoverFillReport;
import fr.insalan.blindtest.cover.CoverFinder;
import fr.insalan.blindtest.cover.CoverFinderTests;
import fr.insalan.blindtest.cover.CoverService;
import fr.insalan.blindtest.model.Game;
import fr.insalan.blindtest.model.Listener;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.repository.GameRepository;
import fr.insalan.blindtest.repository.ListenerRepository;
import fr.insalan.blindtest.repository.TrackRepository;

class GameCoverTests extends IntegrationTest {

    private static final String FRONT = "https://jetta.vgmtreasurechest.com/soundtracks/stellar-blade-soundtrack-2024/00%20Front.jpg";
    private static final String FOLDER = "https://jetta.vgmtreasurechest.com/soundtracks/stellar-blade-soundtrack-2024/folder.jpg";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    CoverService coverService;

    @Autowired
    GameRepository games;

    @Autowired
    TrackRepository tracks;

    @Autowired
    ListenerRepository listeners;

    @Autowired
    AlbumPageStore albumPageStore;

    private HttpServer server;
    private final AtomicInteger cdnRequests = new AtomicInteger();

    @BeforeEach
    void startCdn() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/soundtracks/", exchange -> {
            cdnRequests.incrementAndGet();
            CoverFinderTests.fakeCdn(exchange);
        });
        server.start();
    }

    @AfterEach
    void stopCdn() {
        server.stop(0);
    }

    @Test
    void listsTheImagesOfEachAlbumOfTheGame() throws Exception {
        Game game = stellarBlade();
        track(game, "Dawn", "stellar-blade-soundtrack-2024", "62.%20Dawn.mp3");
        track(game, "Raven", "stellar-blade-soundtrack-2024", "10.%20Raven.mp3");
        Track shael = new Track("Shaël", game);
        shael.setAudioLink(cdn("stellar-blade-mini-original-soundtrack-2024") + "/abcdefgh/01.%20Shael.mp3");
        tracks.save(shael);

        mockMvc.perform(get("/api/admin/games/" + game.getId() + "/cover/candidates").header("Authorization", adminToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].albumUrl").value("https://downloads.khinsider.com/game-soundtracks/album/stellar-blade-soundtrack-2024"))
            .andExpect(jsonPath("$[0].tracks[0]").value("Dawn"))
            .andExpect(jsonPath("$[0].tracks[1]").value("Raven"))
            .andExpect(jsonPath("$[1].tracks[0]").value("Shaël"))
            .andExpect(jsonPath("$[0].images[0]").value(cdn("stellar-blade-soundtrack-2024") + "/00%20Front.jpg"))
            .andExpect(jsonPath("$[0].images[1]").value(cdn("stellar-blade-soundtrack-2024") + "/folder.jpg"))
            .andExpect(jsonPath("$[1].albumUrl").value(cdn("stellar-blade-mini-original-soundtrack-2024")))
            .andExpect(jsonPath("$[1].images.length()").value(0));
    }

    @Test
    void searchesEachAlbumOnceAndGivesItsFirstImageToGamesWithoutOne() {
        Game withAlbum = stellarBlade();
        track(withAlbum, "Dawn", "stellar-blade-soundtrack-2024", "62.%20Dawn.mp3");
        Game alreadyChosen = games.save(new Game("Stellar Blade 2", withAlbum.getFranchise()));
        alreadyChosen.setCoverUrl("https://example.org/chosen.jpg");
        games.save(alreadyChosen);
        track(alreadyChosen, "Democrawler", "stellar-blade-soundtrack-2024", "20.%20Democrawler.mp3");
        Game withoutAlbum = games.save(new Game("Stellar Blade Demo", withAlbum.getFranchise()));
        tracks.save(new Track("Shaël", withoutAlbum));

        assertEquals(new CoverFillReport(1, 1, 1, 0, 2), coverService.fill(CoverFillMode.NEW, report -> {}));
        assertEquals(cdn("stellar-blade-soundtrack-2024") + "/00%20Front.jpg", games.findById(withAlbum.getId()).orElseThrow().getCoverUrl());
        assertEquals("https://example.org/chosen.jpg", games.findById(alreadyChosen.getId()).orElseThrow().getCoverUrl());
        assertNull(games.findById(withoutAlbum.getId()).orElseThrow().getCoverUrl());

        int requests = cdnRequests.get();
        assertEquals(new CoverFillReport(0, 0, 0, 0, 2), coverService.fill(CoverFillMode.NEW, report -> {}));
        assertEquals(requests, cdnRequests.get());
        assertEquals(new CoverFillReport(1, 1, 0, 0, 2), coverService.fill(CoverFillMode.ALL, report -> {}));
    }

    @Test
    void searchesAgainOnlyTheAlbumsWithoutImage() throws Exception {
        Game game = stellarBlade();
        track(game, "Dawn", "stellar-blade-soundtrack-2024", "62.%20Dawn.mp3");
        Game mini = games.save(new Game("Stellar Blade Mini", game.getFranchise()));
        track(mini, "Shaël", "stellar-blade-mini-original-soundtrack-2024", "01.%20Shael.mp3");

        List<CoverFillReport> steps = new ArrayList<>();
        assertEquals(2, coverService.fill(CoverFillMode.NEW, steps::add).scanned());
        assertEquals(new CoverFillReport(1, 2, 1, 0, 1), steps.get(0));
        assertEquals(1, coverService.fill(CoverFillMode.WITHOUT_IMAGE, report -> {}).scanned());

        mockMvc.perform(get("/api/admin/covers/fill").header("Authorization", adminToken()))
            .andExpect(jsonPath("$.albums").value(2))
            .andExpect(jsonPath("$.albumsWithoutImage").value(1));
    }

    @Test
    void theAdminSeesTheStoredImagesAndCanSearchAgain() throws Exception {
        Game game = stellarBlade();
        track(game, "Dawn", "stellar-blade-soundtrack-2024", "62.%20Dawn.mp3");

        mockMvc.perform(get("/api/admin/games/" + game.getId() + "/cover/candidates").header("Authorization", adminToken()))
            .andExpect(jsonPath("$[0].images.length()").value(2));
        int requests = cdnRequests.get();

        mockMvc.perform(get("/api/admin/games/" + game.getId() + "/cover/candidates").header("Authorization", adminToken()))
            .andExpect(jsonPath("$[0].images.length()").value(2));
        assertEquals(requests, cdnRequests.get());

        mockMvc.perform(get("/api/admin/games/" + game.getId() + "/cover/candidates").param("rescan", "true").header("Authorization", adminToken()))
            .andExpect(jsonPath("$[0].images.length()").value(2));
        assertEquals(requests + CoverFinder.COMMON_NAMES.size(), cdnRequests.get());
    }

    @Test
    void savingAnImageChecksTheAddressAndRemovesTheReportsWithoutImage() throws Exception {
        Game game = stellarBlade();
        game.setCoverUrl(FRONT);
        games.save(game);
        Listener nyx = listeners.save(new Listener("Nyx"));
        Listener awing = listeners.save(new Listener("Awing"));
        propose(game, nyx, null).andExpect(status().isNoContent());
        propose(game, awing, FOLDER).andExpect(status().isNoContent());

        mockMvc.perform(put("/api/admin/games/" + game.getId() + "/cover").header("Authorization", adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"coverUrl\": \"pas une adresse\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("L'adresse de l'image doit commencer par http:// ou https://"));

        mockMvc.perform(put("/api/admin/games/" + game.getId() + "/cover").header("Authorization", adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"coverUrl\": \" https://example.org/chosen.jpg \"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.coverUrl").value("https://example.org/chosen.jpg"))
            .andExpect(jsonPath("$.displayUrl").value(startsWith("/api/games/" + game.getId() + "/cover?v=")))
            .andExpect(jsonPath("$.downloaded").value(false));

        mockMvc.perform(get("/api/admin/cover-proposals").header("Authorization", adminToken()))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].listenerName").value("Awing"));

        mockMvc.perform(delete("/api/admin/games/" + game.getId() + "/cover").header("Authorization", adminToken()))
            .andExpect(jsonPath("$.coverUrl").doesNotExist());
    }

    @Test
    void theImageAddressRedirectsToTheOriginalUntilItIsDownloaded() throws Exception {
        Game game = stellarBlade();

        mockMvc.perform(get("/api/games/" + game.getId() + "/cover"))
            .andExpect(status().isNotFound());

        game.setCoverUrl(FRONT);
        games.save(game);
        mockMvc.perform(get("/api/games/" + game.getId() + "/cover"))
            .andExpect(status().isFound())
            .andExpect(header().string("Location", FRONT));
    }

    @Test
    void aPlayerProposesAnImageOrSaysTheImageIsWrong() throws Exception {
        Game game = stellarBlade();
        Listener nyx = listeners.save(new Listener("Nyx"));

        propose(game, nyx, null)
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Ce jeu n'a pas encore d'image : il faut en proposer une"));
        propose(game, nyx, "ftp://image.jpg").andExpect(status().isBadRequest());
        propose(game, nyx, FRONT).andExpect(status().isNoContent());
        propose(game, nyx, FOLDER).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/admin/cover-proposals").header("Authorization", adminToken()))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].game.name").value("Stellar Blade"))
            .andExpect(jsonPath("$[0].currentUrl").doesNotExist())
            .andExpect(jsonPath("$[0].imageUrl").value(FOLDER))
            .andExpect(jsonPath("$[0].listenerName").value("Nyx"));
    }

    @Test
    void acceptingAProposalChangesTheImageAndRemovesTheSameProposals() throws Exception {
        Game game = stellarBlade();
        game.setCoverUrl(FRONT);
        games.save(game);
        Listener nyx = listeners.save(new Listener("Nyx"));
        Listener awing = listeners.save(new Listener("Awing"));
        Listener raven = listeners.save(new Listener("Raven"));
        propose(game, nyx, FOLDER);
        propose(game, awing, FOLDER);
        propose(game, raven, "https://example.org/other.jpg");

        mockMvc.perform(post("/api/admin/cover-proposals/" + game.getId() + "/" + nyx.getId() + "/accept").header("Authorization", adminToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.coverUrl").value(FOLDER));

        assertEquals(FOLDER, games.findById(game.getId()).orElseThrow().getCoverUrl());
        mockMvc.perform(get("/api/admin/cover-proposals").header("Authorization", adminToken()))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].listenerName").value("Raven"));

        mockMvc.perform(delete("/api/admin/cover-proposals/" + game.getId() + "/" + raven.getId()).header("Authorization", adminToken()))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/admin/cover-proposals").header("Authorization", adminToken()))
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void aProposalWithoutImageCannotBeAccepted() throws Exception {
        Game game = stellarBlade();
        game.setCoverUrl(FRONT);
        games.save(game);
        Listener nyx = listeners.save(new Listener("Nyx"));
        propose(game, nyx, "  ");

        mockMvc.perform(post("/api/admin/cover-proposals/" + game.getId() + "/" + nyx.getId() + "/accept").header("Authorization", adminToken()))
            .andExpect(status().isBadRequest());
    }

    @Test
    void proposalsFollowMergedListenersAndLeaveWithDeletedOnes() throws Exception {
        Game game = stellarBlade();
        Listener nyx = listeners.save(new Listener("Nyx"));
        Listener oldNyx = listeners.save(new Listener("NyxCormo"));
        Listener awing = listeners.save(new Listener("Awing"));
        propose(game, oldNyx, FRONT);
        propose(game, awing, FOLDER);

        mockMvc.perform(post("/api/admin/listeners/" + oldNyx.getId() + "/merge").header("Authorization", adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"targetId\": " + nyx.getId() + "}"))
            .andExpect(status().isOk());
        mockMvc.perform(delete("/api/admin/listeners/" + awing.getId()).header("Authorization", adminToken()))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/admin/cover-proposals").header("Authorization", adminToken()))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].listenerName").value("Nyx"))
            .andExpect(jsonPath("$[0].imageUrl").value(FRONT));
    }

    @Test
    void listsTheImageOfEachGameThatHasOne() throws Exception {
        Game withImage = stellarBlade();
        withImage.setCoverUrl(FRONT);
        games.save(withImage);
        games.save(new Game("Stellar Blade 2", withImage.getFranchise()));

        mockMvc.perform(get("/api/admin/covers").header("Authorization", adminToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].gameId").value(withImage.getId()))
            .andExpect(jsonPath("$[0].displayUrl").value(startsWith("/api/games/" + withImage.getId() + "/cover?v=")));
    }

    @Test
    void anAlbumPageOpenedInTheBrowserGivesItsImages() throws Exception {
        Game game = stellarBlade();
        track(game, "Dawn", "stellar-blade-soundtrack-2024", "62.%20Dawn.mp3");

        importPage("https://downloads.khinsider.com/game-soundtracks/album/stellar-blade-soundtrack-2024", "jeton-de-test")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.album").value("stellar-blade-soundtrack-2024"))
            .andExpect(jsonPath("$.images.length()").value(2))
            .andExpect(jsonPath("$.coveredGames[0]").value("Stellar Blade"));

        assertEquals(FRONT, games.findById(game.getId()).orElseThrow().getCoverUrl());
        String saved = albumPageStore.find("stellar-blade-soundtrack-2024").orElseThrow();
        assertTrue(saved.contains("albumImage"));
        assertFalse(saved.contains("albumIconLarge"));
        mockMvc.perform(get("/api/admin/albums").header("Authorization", adminToken()))
            .andExpect(jsonPath("$[0].images[1]").value(FOLDER))
            .andExpect(jsonPath("$[0].pageSavedAt").isNotEmpty())
            .andExpect(jsonPath("$[0].scannedAt").doesNotExist());
    }

    @Test
    void aSavedAlbumPageComesBeforeTheCdnSearch() throws Exception {
        Game game = stellarBlade();
        track(game, "Dawn", "stellar-blade-soundtrack-2024", "62.%20Dawn.mp3");
        Game mini = games.save(new Game("Stellar Blade Mini", game.getFranchise()));
        track(mini, "Shaël", "stellar-blade-mini-original-soundtrack-2024", "01.%20Shael.mp3");
        importPage("https://downloads.khinsider.com/game-soundtracks/album/stellar-blade-soundtrack-2024", "jeton-de-test");

        assertEquals(1, coverService.fill(CoverFillMode.ALL, report -> {}).scanned());

        int requests = cdnRequests.get();
        mockMvc.perform(get("/api/admin/games/" + game.getId() + "/cover/candidates").param("rescan", "true").header("Authorization", adminToken()))
            .andExpect(jsonPath("$[0].fromPage").value(true))
            .andExpect(jsonPath("$[0].images[0]").value(FRONT))
            .andExpect(jsonPath("$[0].images[1]").value(FOLDER));
        assertEquals(requests, cdnRequests.get());

        mockMvc.perform(get("/api/admin/games/" + mini.getId() + "/cover/candidates").header("Authorization", adminToken()))
            .andExpect(jsonPath("$[0].fromPage").value(false))
            .andExpect(jsonPath("$[0].images.length()").value(0));
    }

    @Test
    void anAlbumPageNeedsTheTokenAndAnAlbumOfTheBase() throws Exception {
        Game game = stellarBlade();
        track(game, "Dawn", "stellar-blade-soundtrack-2024", "62.%20Dawn.mp3");

        importPage("https://downloads.khinsider.com/game-soundtracks/album/stellar-blade-soundtrack-2024", "mauvais-jeton")
            .andExpect(status().isForbidden());
        importPage("https://downloads.khinsider.com/game-soundtracks/album/minecraft", "jeton-de-test")
            .andExpect(status().isNotFound());
        importPage("https://example.org/stellar-blade-soundtrack-2024", "jeton-de-test")
            .andExpect(status().isBadRequest());
    }

    @Test
    void listsEveryAlbumWithItsGamesAndSearch() throws Exception {
        Game game = stellarBlade();
        track(game, "Dawn", "stellar-blade-soundtrack-2024", "62.%20Dawn.mp3");
        track(game, "Raven", "stellar-blade-soundtrack-2024", "10.%20Raven.mp3");
        Game mini = games.save(new Game("Stellar Blade Mini", game.getFranchise()));
        track(mini, "Shaël", "stellar-blade-mini-original-soundtrack-2024", "01.%20Shael.mp3");
        coverService.fill(CoverFillMode.NEW, report -> {});
        Game noAlbum = games.save(new Game("Stellar Blade Demo", game.getFranchise()));
        tracks.save(new Track("Democrawler", noAlbum));
        track(game, "Shelter", "stellar-blade-arrange-tracks-original-soundtrack-2025", "02.%20Shelter.mp3");

        mockMvc.perform(get("/api/admin/albums").header("Authorization", adminToken()))
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].name").value("stellar-blade-arrange-tracks-original-soundtrack-2025"))
            .andExpect(jsonPath("$[0].scannedAt").doesNotExist())
            .andExpect(jsonPath("$[1].name").value("stellar-blade-mini-original-soundtrack-2024"))
            .andExpect(jsonPath("$[1].images.length()").value(0))
            .andExpect(jsonPath("$[1].games[0].name").value("Stellar Blade Mini"))
            .andExpect(jsonPath("$[2].pageUrl").value("https://downloads.khinsider.com/game-soundtracks/album/stellar-blade-soundtrack-2024"))
            .andExpect(jsonPath("$[2].tracks.length()").value(2))
            .andExpect(jsonPath("$[2].images.length()").value(2));
    }

    @Test
    void discoverGivesTheImageOfTheGame() throws Exception {
        Game game = stellarBlade();
        game.setCoverUrl(FRONT);
        games.save(game);
        Track dawn = new Track("Dawn", game);
        dawn.setAudioLink("https://example.org/dawn.mp3");
        tracks.save(dawn);
        Listener listener = listeners.save(new Listener("Nyx"));

        mockMvc.perform(get("/api/discover/next").param("listenerId", listener.getId().toString()))
            .andExpect(jsonPath("$.coverUrl").value(startsWith("/api/games/" + game.getId() + "/cover?v=")));
    }

    @Test
    void mergingAGameGivesItsImageToTheTargetWithoutImage() throws Exception {
        Game target = stellarBlade();
        Game source = games.save(new Game("STELLAR BLADE", target.getFranchise()));
        source.setCoverUrl(FRONT);
        games.save(source);

        mockMvc.perform(post("/api/admin/games/" + source.getId() + "/merge").header("Authorization", adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"targetId\": " + target.getId() + "}"))
            .andExpect(status().isOk());

        assertEquals(FRONT, games.findById(target.getId()).orElseThrow().getCoverUrl());
    }

    private ResultActions propose(Game game, Listener listener, String imageUrl) throws Exception {
        String body = imageUrl == null ? "{\"imageUrl\": null}" : "{\"imageUrl\": \"" + imageUrl + "\"}";
        return mockMvc.perform(put("/api/games/" + game.getId() + "/cover-proposal")
            .param("listenerId", listener.getId().toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
    }

    private ResultActions importPage(String albumUrl, String token) throws Exception {
        byte[] html;
        try (InputStream page = getClass().getResourceAsStream("/khinsider/stellar-blade-album.html")) {
            html = page.readAllBytes();
        }
        return mockMvc.perform(post("/api/khinsider/album-page")
            .header("X-Album-Url", albumUrl)
            .header("X-Import-Token", token)
            .contentType(MediaType.TEXT_HTML)
            .content(html));
    }

    private void track(Game game, String name, String album, String file) {
        Track track = new Track(name, game);
        track.setKhinsiderLink("https://downloads.khinsider.com/game-soundtracks/album/" + album + "/" + file.replace("%", "%25"));
        track.setAudioLink(cdn(album) + "/ybomulwy/" + file);
        tracks.save(track);
    }

    private String cdn(String album) {
        return "http://localhost:" + server.getAddress().getPort() + "/soundtracks/" + album;
    }
}
