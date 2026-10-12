package fr.insalan.blindtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import fr.insalan.blindtest.model.Franchise;
import fr.insalan.blindtest.model.Game;
import fr.insalan.blindtest.model.Listener;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.model.TrackReport;
import fr.insalan.blindtest.repository.FranchiseRepository;
import fr.insalan.blindtest.repository.GameRepository;
import fr.insalan.blindtest.repository.ListenerRepository;
import fr.insalan.blindtest.repository.TrackReportRepository;
import fr.insalan.blindtest.repository.TrackRepository;
import fr.insalan.blindtest.security.TokenStore;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminReportTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TokenStore tokenStore;

    @Autowired
    FranchiseRepository franchises;

    @Autowired
    GameRepository games;

    @Autowired
    TrackRepository tracks;

    @Autowired
    ListenerRepository listeners;

    @Autowired
    TrackReportRepository reports;

    @AfterEach
    void cleanDatabase() {
        reports.deleteAll();
        listeners.deleteAll();
        tracks.deleteAll();
        games.deleteAll();
        franchises.deleteAll();
    }

    @Test
    void requiresAnAdminToken() throws Exception {
        mockMvc.perform(get("/api/admin/reports"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/admin/reports/1"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void listsReportedTracksWithTheirReporters() throws Exception {
        Game game = stellarBlade();
        Track dawn = tracks.save(new Track("Dawn", game));
        Track raven = tracks.save(new Track("Raven", game));
        tracks.save(new Track("Democrawler", game));
        Listener nyx = listeners.save(new Listener("Nyx"));
        Listener awing = listeners.save(new Listener("Awing"));
        reports.save(new TrackReport(dawn, nyx));
        reports.save(new TrackReport(raven, nyx));
        reports.save(new TrackReport(dawn, awing));

        mockMvc.perform(get("/api/admin/reports").header("Authorization", adminToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].track.name").value("Dawn"))
            .andExpect(jsonPath("$[0].track.gameName").value("Stellar Blade"))
            .andExpect(jsonPath("$[0].reporters[0]").value("Nyx"))
            .andExpect(jsonPath("$[0].reporters[1]").value("Awing"))
            .andExpect(jsonPath("$[1].track.name").value("Raven"))
            .andExpect(jsonPath("$[1].reporters.length()").value(1));
    }

    @Test
    void dismissesTheReportsOfOneTrack() throws Exception {
        Game game = stellarBlade();
        Track dawn = tracks.save(new Track("Dawn", game));
        Track raven = tracks.save(new Track("Raven", game));
        Listener nyx = listeners.save(new Listener("Nyx"));
        reports.save(new TrackReport(dawn, nyx));
        reports.save(new TrackReport(dawn, listeners.save(new Listener("Awing"))));
        reports.save(new TrackReport(raven, nyx));

        mockMvc.perform(delete("/api/admin/reports/" + dawn.getId()).header("Authorization", adminToken()))
            .andExpect(status().isNoContent());

        assertThat(reports.findAll())
            .singleElement()
            .satisfies(report -> assertThat(report.getTrack().getId()).isEqualTo(raven.getId()));
    }

    @Test
    void removingTheKhinsiderLinkDismissesTheReports() throws Exception {
        Track dawn = new Track("Dawn", stellarBlade());
        dawn.setKhinsiderLink("https://downloads.khinsider.com/dawn");
        dawn = tracks.save(dawn);
        reports.save(new TrackReport(dawn, listeners.save(new Listener("Nyx"))));

        mockMvc.perform(delete("/api/admin/tracks/" + dawn.getId() + "/khinsider-link").header("Authorization", adminToken()))
            .andExpect(status().isOk());

        assertThat(reports.count()).isZero();
    }

    private Game stellarBlade() {
        return games.save(new Game("Stellar Blade", franchises.save(new Franchise("Stellar Blade"))));
    }

    private String adminToken() {
        return "Bearer " + tokenStore.create("admin");
    }
}
