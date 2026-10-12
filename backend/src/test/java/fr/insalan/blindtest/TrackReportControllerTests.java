package fr.insalan.blindtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

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
import fr.insalan.blindtest.model.TrackReportId;
import fr.insalan.blindtest.repository.FranchiseRepository;
import fr.insalan.blindtest.repository.GameRepository;
import fr.insalan.blindtest.repository.ListenerRepository;
import fr.insalan.blindtest.repository.TrackReportRepository;
import fr.insalan.blindtest.repository.TrackRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TrackReportControllerTests {

    @Autowired
    MockMvc mockMvc;

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

    private Track dawn() {
        Game game = games.save(new Game("Stellar Blade", franchises.save(new Franchise("Stellar Blade"))));
        return tracks.save(new Track("Dawn", game));
    }

    private String url(Integer trackId, Integer listenerId) {
        return "/api/tracks/" + trackId + "/reports/" + listenerId;
    }

    @Test
    void reportsATrack() throws Exception {
        Track dawn = dawn();
        Listener nyx = listeners.save(new Listener("Nyx"));

        mockMvc.perform(put(url(dawn.getId(), nyx.getId())))
            .andExpect(status().isNoContent());

        assertThat(reports.existsById(new TrackReportId(dawn.getId(), nyx.getId()))).isTrue();
    }

    @Test
    void reportingTwiceKeepsTheFirstReport() throws Exception {
        Track dawn = dawn();
        Listener nyx = listeners.save(new Listener("Nyx"));
        TrackReportId id = new TrackReportId(dawn.getId(), nyx.getId());

        mockMvc.perform(put(url(dawn.getId(), nyx.getId()))).andExpect(status().isNoContent());
        Instant firstDate = reports.findById(id).orElseThrow().getCreatedAt();
        mockMvc.perform(put(url(dawn.getId(), nyx.getId()))).andExpect(status().isNoContent());

        assertThat(reports.count()).isEqualTo(1);
        assertThat(reports.findById(id).orElseThrow().getCreatedAt()).isEqualTo(firstDate);
    }

    @Test
    void refusesAnUnknownTrackOrListener() throws Exception {
        Track dawn = dawn();
        Listener nyx = listeners.save(new Listener("Nyx"));

        mockMvc.perform(put(url(999999, nyx.getId())))
            .andExpect(status().isNotFound())
            .andExpect(status().reason("Musique introuvable"));
        mockMvc.perform(put(url(dawn.getId(), 999999)))
            .andExpect(status().isNotFound())
            .andExpect(status().reason("Pseudo introuvable"));

        assertThat(reports.count()).isZero();
    }

    @Test
    void tellsHowManyReportsATrackHasAndWhetherTheListenerReportedIt() throws Exception {
        Track dawn = dawn();
        Listener nyx = listeners.save(new Listener("Nyx"));
        Listener awing = listeners.save(new Listener("Awing"));

        mockMvc.perform(get(url(dawn.getId(), nyx.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reports").value(0))
            .andExpect(jsonPath("$.reportedByListener").value(false));

        mockMvc.perform(put(url(dawn.getId(), nyx.getId()))).andExpect(status().isNoContent());

        mockMvc.perform(get(url(dawn.getId(), nyx.getId())))
            .andExpect(jsonPath("$.reports").value(1))
            .andExpect(jsonPath("$.reportedByListener").value(true));
        mockMvc.perform(get(url(dawn.getId(), awing.getId())))
            .andExpect(jsonPath("$.reports").value(1))
            .andExpect(jsonPath("$.reportedByListener").value(false));
    }

    @Test
    void cancelsAReport() throws Exception {
        Track dawn = dawn();
        Listener nyx = listeners.save(new Listener("Nyx"));
        mockMvc.perform(put(url(dawn.getId(), nyx.getId()))).andExpect(status().isNoContent());

        mockMvc.perform(delete(url(dawn.getId(), nyx.getId())))
            .andExpect(status().isNoContent());

        assertThat(reports.count()).isZero();
    }

    @Test
    void cancellingAnUnknownReportIsANoOp() throws Exception {
        mockMvc.perform(delete(url(999999, 999999)))
            .andExpect(status().isNoContent());
    }
}
