package fr.insalan.blindtest;

import static org.junit.jupiter.api.Assertions.assertTrue;
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

import tools.jackson.databind.ObjectMapper;

import fr.insalan.blindtest.dto.GuessFranchiseRequest;
import fr.insalan.blindtest.dto.GuessRequest;
import fr.insalan.blindtest.model.Blindtest;
import fr.insalan.blindtest.model.BlindtestTrack;
import fr.insalan.blindtest.model.Franchise;
import fr.insalan.blindtest.model.Game;
import fr.insalan.blindtest.model.KnowledgeId;
import fr.insalan.blindtest.model.Listener;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.repository.BlindtestDifficultyBandRepository;
import fr.insalan.blindtest.repository.BlindtestRepository;
import fr.insalan.blindtest.repository.BlindtestScoreRepository;
import fr.insalan.blindtest.repository.BlindtestTrackRepository;
import fr.insalan.blindtest.repository.FranchiseRepository;
import fr.insalan.blindtest.repository.GameRepository;
import fr.insalan.blindtest.repository.KnowledgeRepository;
import fr.insalan.blindtest.repository.ListenerRepository;
import fr.insalan.blindtest.repository.TrackRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BlindtestGameControllerTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    BlindtestScoreRepository blindtestScores;

    @Autowired
    BlindtestDifficultyBandRepository blindtestDifficultyBands;

    @Autowired
    BlindtestTrackRepository blindtestTracks;

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
        blindtestScores.deleteAll();
        blindtestDifficultyBands.deleteAll();
        blindtestTracks.deleteAll();
        blindtests.deleteAll();
        knowledge.deleteAll();
        listeners.deleteAll();
        tracks.deleteAll();
        games.deleteAll();
        franchises.deleteAll();
    }

    @Test
    void playsThroughASingleTrackBlindtest() throws Exception {
        Franchise franchise = franchises.save(new Franchise("Stellar Blade"));
        Game game = games.save(new Game("Stellar Blade", franchise));
        Track dawn = tracks.save(new Track("Dawn", game));
        dawn.setAudioLink("https://example.org/dawn");
        tracks.save(dawn);
        Blindtest blindtest = blindtests.save(new Blindtest("Test", 50, 5));
        blindtestTracks.save(new BlindtestTrack(blindtest, dawn, 0));
        Listener listener = listeners.save(new Listener("Nyx"));

        mockMvc.perform(get("/api/blindtests/" + blindtest.getId() + "/session").param("listenerId", listener.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.audioLink").value(dawn.getAudioLink()))
            .andExpect(jsonPath("$.finished").value(false))
            .andExpect(jsonPath("$.totalTracks").value(1))
            .andExpect(jsonPath("$.attemptsRemaining").value(5));

        GuessRequest wrongGuess = new GuessRequest(game.getId() + 1000, null);
        mockMvc.perform(post("/api/blindtests/" + blindtest.getId() + "/guess")
                .param("listenerId", listener.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrongGuess)))
            .andExpect(jsonPath("$.correct").value(false));

        mockMvc.perform(get("/api/blindtests/" + blindtest.getId() + "/session").param("listenerId", listener.getId().toString()))
            .andExpect(jsonPath("$.attemptsRemaining").value(4));

        GuessRequest rightGuess = new GuessRequest(game.getId(), dawn.getId());
        mockMvc.perform(post("/api/blindtests/" + blindtest.getId() + "/guess")
                .param("listenerId", listener.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rightGuess)))
            .andExpect(jsonPath("$.correct").value(true))
            .andExpect(jsonPath("$.bonusCorrect").value(true))
            .andExpect(jsonPath("$.reveal.gameName").value("Stellar Blade"));

        mockMvc.perform(get("/api/blindtests/" + blindtest.getId() + "/session").param("listenerId", listener.getId().toString()))
            .andExpect(jsonPath("$.finished").value(true))
            .andExpect(jsonPath("$.goodAnswers").value(1));
    }

    @Test
    void guessFranchiseAwardsAPointWithoutResolvingTheTrack() throws Exception {
        Franchise franchise = franchises.save(new Franchise("Stellar Blade"));
        Game game = games.save(new Game("Stellar Blade", franchise));
        Track dawn = tracks.save(new Track("Dawn", game));
        dawn.setAudioLink("https://example.org/dawn");
        tracks.save(dawn);
        Blindtest blindtest = blindtests.save(new Blindtest("Test", 50, 5));
        blindtestTracks.save(new BlindtestTrack(blindtest, dawn, 0));
        Listener listener = listeners.save(new Listener("Nyx"));

        GuessFranchiseRequest wrongFranchise = new GuessFranchiseRequest(franchise.getId() + 1000);
        mockMvc.perform(post("/api/blindtests/" + blindtest.getId() + "/guess-franchise")
                .param("listenerId", listener.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrongFranchise)))
            .andExpect(jsonPath("$.correct").value(false));

        GuessFranchiseRequest rightFranchise = new GuessFranchiseRequest(franchise.getId());
        mockMvc.perform(post("/api/blindtests/" + blindtest.getId() + "/guess-franchise")
                .param("listenerId", listener.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rightFranchise)))
            .andExpect(jsonPath("$.correct").value(true));

        mockMvc.perform(get("/api/blindtests/" + blindtest.getId() + "/session").param("listenerId", listener.getId().toString()))
            .andExpect(jsonPath("$.finished").value(false))
            .andExpect(jsonPath("$.trackId").value(dawn.getId()));
    }

    @Test
    void exhaustingAttemptsRevealsTheTrackWithoutCountingAsCorrect() throws Exception {
        Franchise franchise = franchises.save(new Franchise("Stellar Blade"));
        Game game = games.save(new Game("Stellar Blade", franchise));
        Track dawn = tracks.save(new Track("Dawn", game));
        dawn.setAudioLink("https://example.org/dawn");
        tracks.save(dawn);
        Blindtest blindtest = blindtests.save(new Blindtest("Test", 50, 2));
        blindtestTracks.save(new BlindtestTrack(blindtest, dawn, 0));
        Listener listener = listeners.save(new Listener("Nyx"));

        GuessRequest wrongGuess = new GuessRequest(game.getId() + 1000, null);
        mockMvc.perform(post("/api/blindtests/" + blindtest.getId() + "/guess")
                .param("listenerId", listener.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrongGuess)))
            .andExpect(jsonPath("$.correct").value(false))
            .andExpect(jsonPath("$.reveal").doesNotExist());

        mockMvc.perform(post("/api/blindtests/" + blindtest.getId() + "/guess")
                .param("listenerId", listener.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrongGuess)))
            .andExpect(jsonPath("$.correct").value(false))
            .andExpect(jsonPath("$.reveal.gameName").value("Stellar Blade"));

        mockMvc.perform(get("/api/blindtests/" + blindtest.getId() + "/session").param("listenerId", listener.getId().toString()))
            .andExpect(jsonPath("$.finished").value(true))
            .andExpect(jsonPath("$.goodAnswers").value(0));
    }

    @Test
    void passThenKnowAnywayCorrectsKnowledgeOnly() throws Exception {
        Franchise franchise = franchises.save(new Franchise("Stellar Blade"));
        Game game = games.save(new Game("Stellar Blade", franchise));
        Track dawn = tracks.save(new Track("Dawn", game));
        dawn.setAudioLink("https://example.org/dawn");
        tracks.save(dawn);
        Blindtest blindtest = blindtests.save(new Blindtest("Test", 50, 5));
        blindtestTracks.save(new BlindtestTrack(blindtest, dawn, 0));
        Listener listener = listeners.save(new Listener("Nyx"));

        mockMvc.perform(post("/api/blindtests/" + blindtest.getId() + "/pass")
                .param("listenerId", listener.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.gameName").value("Stellar Blade"))
            .andExpect(jsonPath("$.trackId").value(dawn.getId()));

        mockMvc.perform(post("/api/blindtests/" + blindtest.getId() + "/know-anyway")
                .param("listenerId", listener.getId().toString())
                .param("trackId", dawn.getId().toString()))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/blindtests/" + blindtest.getId() + "/session").param("listenerId", listener.getId().toString()))
            .andExpect(jsonPath("$.finished").value(true))
            .andExpect(jsonPath("$.goodAnswers").value(0));

        assertKnowsDawn(listener, dawn);
    }

    @Test
    void leaderboardIsSortedByGoodAnswers() throws Exception {
        Franchise franchise = franchises.save(new Franchise("Stellar Blade"));
        Game game = games.save(new Game("Stellar Blade", franchise));
        Track dawn = tracks.save(new Track("Dawn", game));
        dawn.setAudioLink("https://example.org/dawn");
        tracks.save(dawn);
        Track raven = tracks.save(new Track("Raven", game));
        raven.setAudioLink("https://example.org/raven");
        tracks.save(raven);
        Blindtest blindtest = blindtests.save(new Blindtest("Test", 50, 5));
        blindtestTracks.save(new BlindtestTrack(blindtest, dawn, 0));
        blindtestTracks.save(new BlindtestTrack(blindtest, raven, 1));
        Listener bonneReponse = listeners.save(new Listener("BonneReponse"));
        Listener mauvaiseReponse = listeners.save(new Listener("MauvaiseReponse"));

        GuessRequest rightGuess = new GuessRequest(game.getId(), null);
        mockMvc.perform(post("/api/blindtests/" + blindtest.getId() + "/guess")
                .param("listenerId", bonneReponse.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rightGuess)))
            .andExpect(jsonPath("$.correct").value(true));

        mockMvc.perform(post("/api/blindtests/" + blindtest.getId() + "/pass")
                .param("listenerId", mauvaiseReponse.getId().toString()))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/blindtests/" + blindtest.getId() + "/leaderboard"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].listenerName").value("BonneReponse"))
            .andExpect(jsonPath("$[0].goodAnswers").value(1))
            .andExpect(jsonPath("$[0].tracksHeard").value(1))
            .andExpect(jsonPath("$[1].listenerName").value("MauvaiseReponse"))
            .andExpect(jsonPath("$[1].goodAnswers").value(0));
    }

    private void assertKnowsDawn(Listener listener, Track dawn) {
        boolean knows = knowledge.findById(new KnowledgeId(listener.getId(), dawn.getId())).orElseThrow().isKnows();
        assertTrue(knows);
    }
}
