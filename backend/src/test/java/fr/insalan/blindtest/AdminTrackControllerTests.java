package fr.insalan.blindtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.sun.net.httpserver.HttpServer;
import tools.jackson.databind.ObjectMapper;

import fr.insalan.blindtest.dto.SetLinkRequest;
import fr.insalan.blindtest.model.Franchise;
import fr.insalan.blindtest.model.Game;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.repository.FranchiseRepository;
import fr.insalan.blindtest.repository.GameRepository;
import fr.insalan.blindtest.repository.TrackRepository;
import fr.insalan.blindtest.security.TokenStore;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminTrackControllerTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    TokenStore tokenStore;

    @Autowired
    FranchiseRepository franchises;

    @Autowired
    GameRepository games;

    @Autowired
    TrackRepository tracks;

    @AfterEach
    void cleanDatabase() {
        tracks.deleteAll();
        games.deleteAll();
        franchises.deleteAll();
    }

    @Test
    void requiresAnAdminToken() throws Exception {
        Track dawn = dawn();

        mockMvc.perform(delete("/api/admin/tracks/" + dawn.getId() + "/youtube-link"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void replacesAnExistingYoutubeLink() throws Exception {
        Track dawn = dawn();
        dawn.setYoutubeLink("https://www.youtube.com/watch?v=ancien");
        tracks.save(dawn);

        mockMvc.perform(put("/api/admin/tracks/" + dawn.getId() + "/youtube-link")
                .header("Authorization", adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SetLinkRequest("https://www.youtube.com/watch?v=nouveau"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.youtubeLink").value("https://www.youtube.com/watch?v=nouveau"));
    }

    @Test
    void stillRejectsALinkUsedByAnotherTrack() throws Exception {
        Track dawn = dawn();
        dawn.setYoutubeLink("https://www.youtube.com/watch?v=exemple");
        tracks.save(dawn);
        Track raven = tracks.save(new Track("Raven", dawn.getGame()));

        mockMvc.perform(put("/api/admin/tracks/" + raven.getId() + "/youtube-link")
                .header("Authorization", adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SetLinkRequest("https://www.youtube.com/watch?v=exemple"))))
            .andExpect(status().isConflict());
    }

    @Test
    void removingTheKhinsiderLinkAlsoRemovesTheAudioLink() throws Exception {
        Track dawn = dawn();
        dawn.setKhinsiderLink("https://downloads.khinsider.com/dawn");
        dawn.setAudioLink("https://exemple.com/dawn.mp3");
        tracks.save(dawn);

        mockMvc.perform(delete("/api/admin/tracks/" + dawn.getId() + "/khinsider-link").header("Authorization", adminToken()))
            .andExpect(status().isOk());

        Track updated = tracks.findById(dawn.getId()).orElseThrow();
        assertThat(updated.getKhinsiderLink()).isNull();
        assertThat(updated.getAudioLink()).isNull();
    }

    @Test
    void replacingTheKhinsiderLinkResolvesANewAudioLink() throws Exception {
        // "khinsider.com" dans le chemin, pour passer la vérification de domaine sur ce serveur local de test.
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/ancien.mp3", exchange -> {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.createContext("/khinsider.com/nouveau", exchange -> {
            String audioUrl = "http://localhost:" + exchange.getLocalAddress().getPort() + "/nouveau.mp3";
            byte[] bytes = ("<html><body><audio id=\"audio\" src=\"" + audioUrl + "\"></audio></body></html>")
                .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            String base = "http://localhost:" + server.getAddress().getPort();
            Track dawn = dawn();
            dawn.setKhinsiderLink(base + "/khinsider.com/ancien");
            dawn.setAudioLink(base + "/ancien.mp3");
            tracks.save(dawn);

            mockMvc.perform(put("/api/admin/tracks/" + dawn.getId() + "/khinsider-link")
                    .header("Authorization", adminToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new SetLinkRequest(base + "/khinsider.com/nouveau"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.audioLink").value(base + "/nouveau.mp3"));
        } finally {
            server.stop(0);
        }
    }

    private Track dawn() {
        Franchise stellar = franchises.save(new Franchise("Stellar Blade"));
        Game game = games.save(new Game("Stellar Blade", stellar));
        return tracks.save(new Track("Dawn", game));
    }

    private String adminToken() {
        return "Bearer " + tokenStore.create("admin");
    }
}
