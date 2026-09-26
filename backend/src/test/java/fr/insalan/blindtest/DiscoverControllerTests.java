package fr.insalan.blindtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import fr.insalan.blindtest.model.Knowledge;
import fr.insalan.blindtest.model.KnowledgeId;
import fr.insalan.blindtest.model.Listener;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.repository.FranchiseRepository;
import fr.insalan.blindtest.repository.GameRepository;
import fr.insalan.blindtest.repository.KnowledgeRepository;
import fr.insalan.blindtest.repository.ListenerRepository;
import fr.insalan.blindtest.repository.TrackRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DiscoverControllerTests {

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
    KnowledgeRepository knowledge;

    @AfterEach
    void cleanDatabase() {
        knowledge.deleteAll();
        listeners.deleteAll();
        tracks.deleteAll();
        games.deleteAll();
        franchises.deleteAll();
    }

    private Track playableTrack(Game game, String name) {
        Track track = tracks.save(new Track(name, game));
        track.setAudioLink("https://example.org/" + name);
        tracks.save(track);
        return track;
    }

    @Test
    void returnsAnUnvotedPlayableTrack() throws Exception {
        Franchise franchise = franchises.save(new Franchise("Stellar Blade"));
        Game game = games.save(new Game("Stellar Blade", franchise));
        Track dawn = playableTrack(game, "Dawn");
        Listener listener = listeners.save(new Listener("Nyx"));

        mockMvc.perform(get("/api/discover/next").param("listenerId", listener.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.trackId").value(dawn.getId()))
            .andExpect(jsonPath("$.audioLink").value(dawn.getAudioLink()))
            .andExpect(jsonPath("$.franchiseName").value("Stellar Blade"))
            .andExpect(jsonPath("$.gameId").value(game.getId()))
            .andExpect(jsonPath("$.gameName").value("Stellar Blade"))
            .andExpect(jsonPath("$.trackName").value("Dawn"))
            .andExpect(jsonPath("$.finished").value(false));
    }

    @Test
    void neverReturnsATrackAlreadyVotedByThisListener() throws Exception {
        Franchise franchise = franchises.save(new Franchise("Stellar Blade"));
        Game game = games.save(new Game("Stellar Blade", franchise));
        Track dawn = playableTrack(game, "Dawn");
        Track raven = playableTrack(game, "Raven");
        Listener listener = listeners.save(new Listener("Nyx"));
        knowledge.save(new Knowledge(listener, dawn, true));

        mockMvc.perform(get("/api/discover/next").param("listenerId", listener.getId().toString()))
            .andExpect(jsonPath("$.trackId").value(raven.getId()));
    }

    @Test
    void ignoresVotesFromOtherListeners() throws Exception {
        Franchise franchise = franchises.save(new Franchise("Stellar Blade"));
        Game game = games.save(new Game("Stellar Blade", franchise));
        Track dawn = playableTrack(game, "Dawn");
        Listener other = listeners.save(new Listener("Other"));
        Listener listener = listeners.save(new Listener("Nyx"));
        knowledge.save(new Knowledge(other, dawn, true));

        mockMvc.perform(get("/api/discover/next").param("listenerId", listener.getId().toString()))
            .andExpect(jsonPath("$.trackId").value(dawn.getId()));
    }

    @Test
    void ignoresTracksWithoutAudioLink() throws Exception {
        Franchise franchise = franchises.save(new Franchise("Stellar Blade"));
        Game game = games.save(new Game("Stellar Blade", franchise));
        tracks.save(new Track("Silent", game));
        Listener listener = listeners.save(new Listener("Nyx"));

        mockMvc.perform(get("/api/discover/next").param("listenerId", listener.getId().toString()))
            .andExpect(jsonPath("$.finished").value(true));
    }

    @Test
    void returnsFinishedWhenEverythingIsVoted() throws Exception {
        Franchise franchise = franchises.save(new Franchise("Stellar Blade"));
        Game game = games.save(new Game("Stellar Blade", franchise));
        Track dawn = playableTrack(game, "Dawn");
        Listener listener = listeners.save(new Listener("Nyx"));
        knowledge.save(new Knowledge(listener, dawn, true));

        mockMvc.perform(get("/api/discover/next").param("listenerId", listener.getId().toString()))
            .andExpect(jsonPath("$.finished").value(true))
            .andExpect(jsonPath("$.trackId").doesNotExist())
            .andExpect(jsonPath("$.audioLink").doesNotExist());
    }

    @Test
    void setKnowledgeRecordsKnowsTrue() throws Exception {
        Franchise franchise = franchises.save(new Franchise("Stellar Blade"));
        Game game = games.save(new Game("Stellar Blade", franchise));
        Track dawn = playableTrack(game, "Dawn");
        Listener listener = listeners.save(new Listener("Nyx"));

        mockMvc.perform(post("/api/discover/knowledge")
                .param("listenerId", listener.getId().toString())
                .param("trackId", dawn.getId().toString())
                .param("knows", "true"))
            .andExpect(status().isNoContent());

        assertThat(knowledge.findById(new KnowledgeId(listener.getId(), dawn.getId())).orElseThrow().isKnows()).isTrue();
    }

    @Test
    void setKnowledgeOverwritesAPreviousVote() throws Exception {
        Franchise franchise = franchises.save(new Franchise("Stellar Blade"));
        Game game = games.save(new Game("Stellar Blade", franchise));
        Track dawn = playableTrack(game, "Dawn");
        Listener listener = listeners.save(new Listener("Nyx"));
        knowledge.save(new Knowledge(listener, dawn, true));

        mockMvc.perform(post("/api/discover/knowledge")
                .param("listenerId", listener.getId().toString())
                .param("trackId", dawn.getId().toString())
                .param("knows", "false"))
            .andExpect(status().isNoContent());

        assertThat(knowledge.findById(new KnowledgeId(listener.getId(), dawn.getId())).orElseThrow().isKnows()).isFalse();
    }

    @Test
    void listsKnowledgeForAGivenListenerOnly() throws Exception {
        Franchise franchise = franchises.save(new Franchise("Stellar Blade"));
        Game game = games.save(new Game("Stellar Blade", franchise));
        Track dawn = playableTrack(game, "Dawn");
        Track raven = playableTrack(game, "Raven");
        Listener listener = listeners.save(new Listener("Nyx"));
        Listener other = listeners.save(new Listener("Other"));
        knowledge.save(new Knowledge(listener, dawn, true));
        knowledge.save(new Knowledge(listener, raven, false));
        knowledge.save(new Knowledge(other, dawn, false));

        mockMvc.perform(get("/api/discover/knowledge").param("listenerId", listener.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[?(@.trackId == " + dawn.getId() + ")].knows").value(true))
            .andExpect(jsonPath("$[?(@.trackId == " + raven.getId() + ")].knows").value(false));
    }

    @Test
    void listsNothingForAListenerWithoutAnyVote() throws Exception {
        Listener listener = listeners.save(new Listener("Nyx"));

        mockMvc.perform(get("/api/discover/knowledge").param("listenerId", listener.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deleteKnowledgeRemovesTheVote() throws Exception {
        Franchise franchise = franchises.save(new Franchise("Stellar Blade"));
        Game game = games.save(new Game("Stellar Blade", franchise));
        Track dawn = playableTrack(game, "Dawn");
        Listener listener = listeners.save(new Listener("Nyx"));
        knowledge.save(new Knowledge(listener, dawn, true));

        mockMvc.perform(delete("/api/discover/knowledge")
                .param("listenerId", listener.getId().toString())
                .param("trackId", dawn.getId().toString()))
            .andExpect(status().isNoContent());

        assertThat(knowledge.findById(new KnowledgeId(listener.getId(), dawn.getId()))).isEmpty();
    }

    @Test
    void deletingAnUnknownVoteIsANoOp() throws Exception {
        Franchise franchise = franchises.save(new Franchise("Stellar Blade"));
        Game game = games.save(new Game("Stellar Blade", franchise));
        Track dawn = playableTrack(game, "Dawn");
        Listener listener = listeners.save(new Listener("Nyx"));

        mockMvc.perform(delete("/api/discover/knowledge")
                .param("listenerId", listener.getId().toString())
                .param("trackId", dawn.getId().toString()))
            .andExpect(status().isNoContent());
    }
}
