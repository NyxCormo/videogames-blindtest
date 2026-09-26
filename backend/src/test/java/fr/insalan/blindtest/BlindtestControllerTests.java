package fr.insalan.blindtest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;


import java.util.List;

import fr.insalan.blindtest.dto.CreateBlindtestRequest;
import fr.insalan.blindtest.dto.DifficultyBandRequest;
import fr.insalan.blindtest.model.Franchise;
import fr.insalan.blindtest.model.Game;
import fr.insalan.blindtest.model.Knowledge;
import fr.insalan.blindtest.model.Listener;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.repository.BlindtestDifficultyBandRepository;
import fr.insalan.blindtest.repository.BlindtestRepository;
import fr.insalan.blindtest.repository.BlindtestTrackRepository;
import fr.insalan.blindtest.repository.FranchiseRepository;
import fr.insalan.blindtest.repository.GameRepository;
import fr.insalan.blindtest.repository.KnowledgeRepository;
import fr.insalan.blindtest.repository.ListenerRepository;
import fr.insalan.blindtest.repository.TrackRepository;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BlindtestControllerTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    BlindtestTrackRepository blindtestTracks;

    @Autowired
    BlindtestDifficultyBandRepository blindtestDifficultyBands;

    @Autowired
    BlindtestRepository blindtests;

    @Autowired
    KnowledgeRepository knowledge;

    @Autowired
    ListenerRepository listeners;

    @Autowired
    TrackRepository tracks;

    @Autowired
    GameRepository games;

    @Autowired
    FranchiseRepository franchises;

    @AfterEach
    void cleanDatabase() {
        blindtestTracks.deleteAll();
        blindtestDifficultyBands.deleteAll();
        blindtests.deleteAll();
        knowledge.deleteAll();
        listeners.deleteAll();
        tracks.deleteAll();
        games.deleteAll();
        franchises.deleteAll();
    }

    private void playableTrack(Game game, String name, boolean known) {
        Track track = tracks.save(new Track(name, game));
        track.setAudioLink("https://example.org/" + name);
        tracks.save(track);
        Listener listener = listeners.save(new Listener(name + "-listener"));
        knowledge.save(new Knowledge(listener, track, known));
    }

    @Test
    void listIsEmptyWithoutData() throws Exception {
        mockMvc.perform(get("/api/blindtests"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    private static final List<DifficultyBandRequest> ANY_DIFFICULTY = List.of(new DifficultyBandRequest(0, 100, 100));

    @Test
    void createsAndListsABlindtest() throws Exception {
        Franchise franchise = franchises.save(new Franchise("Stellar Blade"));
        Game game = games.save(new Game("Stellar Blade", franchise));
        playableTrack(game, "Dawn", true);

        CreateBlindtestRequest request = new CreateBlindtestRequest("Soirée InsaLan", 1, ANY_DIFFICULTY, null, null, null, null, null, null);

        mockMvc.perform(post("/api/blindtests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Soirée InsaLan"))
            .andExpect(jsonPath("$.difficulty").value(50));

        mockMvc.perform(get("/api/blindtests"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Soirée InsaLan"));
    }

    @Test
    void rejectsBlankName() throws Exception {
        CreateBlindtestRequest request = new CreateBlindtestRequest(" ", 1, ANY_DIFFICULTY, null, null, null, null, null, null);

        mockMvc.perform(post("/api/blindtests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsWhenNotEnoughTracks() throws Exception {
        CreateBlindtestRequest request = new CreateBlindtestRequest("Soirée InsaLan", 5, ANY_DIFFICULTY, null, null, null, null, null, null);

        mockMvc.perform(post("/api/blindtests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsWhenNoBandsProvided() throws Exception {
        CreateBlindtestRequest request = new CreateBlindtestRequest("Soirée InsaLan", 1, List.of(), null, null, null, null, null, null);

        mockMvc.perform(post("/api/blindtests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsWhenProportionsDoNotSumTo100() throws Exception {
        List<DifficultyBandRequest> bands = List.of(
            new DifficultyBandRequest(0, 50, 40),
            new DifficultyBandRequest(50, 100, 40)
        );
        CreateBlindtestRequest request = new CreateBlindtestRequest("Soirée InsaLan", 1, bands, null, null, null, null, null, null);

        mockMvc.perform(post("/api/blindtests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsUnknownStrategy() throws Exception {
        CreateBlindtestRequest request = new CreateBlindtestRequest("Soirée InsaLan", 1, ANY_DIFFICULTY, null, null, null, null, "n'importe quoi", null);

        mockMvc.perform(post("/api/blindtests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsNonPositiveMaxAttempts() throws Exception {
        CreateBlindtestRequest request = new CreateBlindtestRequest("Soirée InsaLan", 1, ANY_DIFFICULTY, null, null, null, null, null, 0);

        mockMvc.perform(post("/api/blindtests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}
