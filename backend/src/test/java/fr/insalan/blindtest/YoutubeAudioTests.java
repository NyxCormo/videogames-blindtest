package fr.insalan.blindtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.sun.net.httpserver.HttpServer;

import fr.insalan.blindtest.audiolink.AudioLinkRefreshService;
import fr.insalan.blindtest.model.Game;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.repository.TrackRepository;
import fr.insalan.blindtest.youtube.AudioDownloader;
import fr.insalan.blindtest.youtube.YoutubeAudioJob;
import fr.insalan.blindtest.youtube.YoutubeAudioReport;
import fr.insalan.blindtest.youtube.YoutubeAudioService;

@TestPropertySource(properties = "youtube.downloads-enabled=true")
@Import(YoutubeAudioTests.FakeDownloaderConfig.class)
class YoutubeAudioTests extends IntegrationTest {

    static final byte[] AUDIO = "faux audio de Dawn".getBytes(StandardCharsets.UTF_8);

    static class FakeDownloader implements AudioDownloader {

        final AtomicInteger calls = new AtomicInteger();
        volatile boolean broken;
        volatile Runnable during;

        @Override
        public Path download(String youtubeUrl, Path directory) throws IOException {
            calls.incrementAndGet();
            if (during != null) {
                during.run();
            }
            if (broken || youtubeUrl.contains("indisponible")) {
                throw new IOException("Vidéo indisponible");
            }
            Path file = directory.resolve(youtubeUrl.contains("texte") ? "audio.txt" : "audio.m4a");
            Files.write(file, AUDIO);
            return file;
        }
    }

    @TestConfiguration
    static class FakeDownloaderConfig {

        @Bean
        @Primary
        FakeDownloader fakeDownloader() {
            return new FakeDownloader();
        }
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    FakeDownloader downloader;

    @Autowired
    YoutubeAudioService youtubeAudioService;

    @Autowired
    YoutubeAudioJob youtubeAudioJob;

    @Autowired
    AudioLinkRefreshService audioLinkRefreshService;

    @Autowired
    TrackRepository tracks;

    private HttpServer khinsider;

    @BeforeEach
    void reset() throws IOException {
        downloader.calls.set(0);
        downloader.broken = false;
        downloader.during = null;
        khinsider = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        khinsider.createContext("/album/sans-son", exchange -> {
            byte[] page = "<html><body>Musique indisponible</body></html>".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, page.length);
            exchange.getResponseBody().write(page);
            exchange.close();
        });
        khinsider.start();
    }

    @AfterEach
    void stopKhinsider() {
        khinsider.stop(0);
    }

    @Test
    void downloadsTheMissingAudioAndServesIt() throws Exception {
        Track dawn = youtubeTrack("Dawn", "https://www.youtube.com/watch?v=dawn");

        YoutubeAudioReport report = youtubeAudioService.downloadMissing(progress -> {});

        assertEquals(new YoutubeAudioReport(1, 1, 1, 0), report);
        Track saved = tracks.findById(dawn.getId()).orElseThrow();
        assertEquals(dawn.getId() + ".m4a", saved.getYoutubeAudioFile());
        assertEquals("/api/tracks/" + dawn.getId() + "/audio", saved.getAudioLink());
        mockMvc.perform(get("/api/tracks/" + dawn.getId() + "/audio"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "audio/mp4"))
            .andExpect(content().bytes(AUDIO));
        mockMvc.perform(get("/api/tracks/" + dawn.getId() + "/audio").header("Range", "bytes=0-3"))
            .andExpect(status().isPartialContent())
            .andExpect(content().bytes("faux".getBytes(StandardCharsets.UTF_8)));

        assertEquals(new YoutubeAudioReport(0, 0, 0, 0), youtubeAudioService.downloadMissing(progress -> {}));
        assertEquals(1, downloader.calls.get());
    }

    @Test
    void khinsiderComesFirstAndYoutubeTakesOverWhenItsLinkIsDead() {
        Track raven = youtubeTrack("Raven", "https://www.youtube.com/watch?v=raven");
        raven.setKhinsiderLink("https://downloads.khinsider.com/game-soundtracks/album/stellar-blade/raven.mp3");
        raven.setAudioLink("https://example.org/raven.mp3");
        tracks.save(raven);

        youtubeAudioService.downloadMissing(progress -> {});
        assertEquals(0, downloader.calls.get());

        raven.setAudioLink(null);
        tracks.save(raven);
        youtubeAudioService.downloadMissing(progress -> {});
        assertEquals(1, downloader.calls.get());
        assertEquals("/api/tracks/" + raven.getId() + "/audio", tracks.findById(raven.getId()).orElseThrow().getAudioLink());
    }

    @Test
    void aFailureIsShownToTheAdminAndRetriedNextTime() throws Exception {
        Track shael = youtubeTrack("Shaël", "https://www.youtube.com/watch?v=shael");
        downloader.broken = true;

        assertEquals(new YoutubeAudioReport(1, 1, 0, 1), youtubeAudioService.downloadMissing(progress -> {}));
        assertNull(tracks.findById(shael.getId()).orElseThrow().getAudioLink());
        mockMvc.perform(get("/api/admin/youtube-audio").header("Authorization", adminToken()))
            .andExpect(jsonPath("$.available").value(true))
            .andExpect(jsonPath("$.tracks").value(1))
            .andExpect(jsonPath("$.downloaded").value(0))
            .andExpect(jsonPath("$.failures[0].trackName").value("Shaël"))
            .andExpect(jsonPath("$.failures[0].gameName").value("Stellar Blade"))
            .andExpect(jsonPath("$.failures[0].error").value("Vidéo indisponible"));

        downloader.broken = false;
        assertEquals(new YoutubeAudioReport(1, 1, 1, 0), youtubeAudioService.downloadMissing(progress -> {}));
        assertNull(tracks.findById(shael.getId()).orElseThrow().getYoutubeAudioError());
    }

    @Test
    void aLinkChangedDuringTheDownloadIsKept() {
        Track dawn = youtubeTrack("Dawn", "https://www.youtube.com/watch?v=dawn");
        downloader.during = () -> {
            Track edited = tracks.findById(dawn.getId()).orElseThrow();
            edited.setYoutubeLink("https://www.youtube.com/watch?v=dawn-officiel");
            tracks.save(edited);
        };

        youtubeAudioService.downloadMissing(progress -> {});

        Track saved = tracks.findById(dawn.getId()).orElseThrow();
        assertEquals("https://www.youtube.com/watch?v=dawn-officiel", saved.getYoutubeLink());
        assertNull(saved.getYoutubeAudioFile());
        assertNull(saved.getAudioLink());
    }

    @Test
    void refusesAFileThatIsNotAudio() {
        Track democrawler = youtubeTrack("Democrawler", "https://www.youtube.com/watch?v=texte");

        youtubeAudioService.downloadMissing(progress -> {});

        Track saved = tracks.findById(democrawler.getId()).orElseThrow();
        assertNull(saved.getYoutubeAudioFile());
        assertTrue(saved.getYoutubeAudioError().startsWith("Format audio non accepté"));
    }

    @Test
    void addingAYoutubeLinkDownloadsItAndRemovingItDeletesTheFile() throws Exception {
        Track dawn = tracks.save(new Track("Dawn", stellarBlade()));

        mockMvc.perform(put("/api/admin/tracks/" + dawn.getId() + "/youtube-link").header("Authorization", adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"link\": \"https://www.youtube.com/watch?v=dawn\"}"))
            .andExpect(status().isOk());
        waitForTheJob();

        Track saved = tracks.findById(dawn.getId()).orElseThrow();
        assertEquals(dawn.getId() + ".m4a", saved.getYoutubeAudioFile());
        assertEquals("/api/tracks/" + dawn.getId() + "/audio", saved.getAudioLink());

        mockMvc.perform(delete("/api/admin/tracks/" + dawn.getId() + "/youtube-link").header("Authorization", adminToken()))
            .andExpect(status().isOk());

        Track removed = tracks.findById(dawn.getId()).orElseThrow();
        assertNull(removed.getYoutubeAudioFile());
        assertNull(removed.getAudioLink());
        mockMvc.perform(get("/api/tracks/" + dawn.getId() + "/audio"))
            .andExpect(status().isNotFound());
    }

    @Test
    void theDailyKhinsiderCheckDoesNotBreakOnADownloadedAudio() {
        Track raven = youtubeTrack("Raven", "https://www.youtube.com/watch?v=raven");
        raven.setKhinsiderLink("http://localhost:" + khinsider.getAddress().getPort() + "/album/sans-son");
        tracks.save(raven);
        youtubeAudioService.downloadMissing(progress -> {});

        audioLinkRefreshService.refresh();
        assertNull(tracks.findById(raven.getId()).orElseThrow().getAudioLink());

        youtubeAudioService.downloadMissing(progress -> {});
        assertEquals("/api/tracks/" + raven.getId() + "/audio", tracks.findById(raven.getId()).orElseThrow().getAudioLink());
        assertEquals(1, downloader.calls.get());
    }

    private Track youtubeTrack(String name, String youtubeLink) {
        Track track = new Track(name, stellarBlade());
        track.setYoutubeLink(youtubeLink);
        return tracks.save(track);
    }

    private void waitForTheJob() throws InterruptedException {
        for (int i = 0; i < 50 && youtubeAudioJob.status().running(); i++) {
            Thread.sleep(100);
        }
        assertFalse(youtubeAudioJob.status().running());
    }
}
