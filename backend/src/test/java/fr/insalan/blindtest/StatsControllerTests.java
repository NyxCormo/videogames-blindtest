package fr.insalan.blindtest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import fr.insalan.blindtest.model.Franchise;
import fr.insalan.blindtest.model.Game;
import fr.insalan.blindtest.model.Knowledge;
import fr.insalan.blindtest.model.Listener;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.repository.FranchiseRepository;
import fr.insalan.blindtest.repository.GameRepository;
import fr.insalan.blindtest.repository.KnowledgeRepository;
import fr.insalan.blindtest.repository.ListenerRepository;
import fr.insalan.blindtest.repository.TrackRepository;

class StatsControllerTests extends IntegrationTest {

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

    private Track track(Game game, String name, boolean playable) {
        Track track = new Track(name, game);
        if (playable) {
            track.setAudioLink("https://example.org/" + name);
        }
        return tracks.save(track);
    }

    @Test
    void countsEverythingInTheDatabase() throws Exception {
        Game game = games.save(new Game("Stellar Blade", franchises.save(new Franchise("Stellar Blade"))));
        Track dawn = track(game, "Dawn", true);
        track(game, "Raven", true);
        track(game, "Democrawler", false);
        Listener nyx = listeners.save(new Listener("Nyx"));
        listeners.save(new Listener("Awing"));
        knowledge.save(new Knowledge(nyx, dawn, true));

        mockMvc.perform(get("/api/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tracks").value(3))
            .andExpect(jsonPath("$.playableTracks").value(2))
            .andExpect(jsonPath("$.games").value(1))
            .andExpect(jsonPath("$.franchises").value(1))
            .andExpect(jsonPath("$.listeners").value(2))
            .andExpect(jsonPath("$.votes").value(1));
    }

    @Test
    void returnsZerosOnAnEmptyDatabase() throws Exception {
        mockMvc.perform(get("/api/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tracks").value(0))
            .andExpect(jsonPath("$.votes").value(0));
    }

    @Test
    void countsTheVotesOfOneListener() throws Exception {
        Game game = games.save(new Game("Stellar Blade", franchises.save(new Franchise("Stellar Blade"))));
        Track dawn = track(game, "Dawn", true);
        Track raven = track(game, "Raven", true);
        track(game, "Shaël", true);
        Track democrawler = track(game, "Democrawler", false);
        Listener nyx = listeners.save(new Listener("Nyx"));
        Listener awing = listeners.save(new Listener("Awing"));
        knowledge.save(new Knowledge(nyx, dawn, true));
        knowledge.save(new Knowledge(nyx, raven, false));
        knowledge.save(new Knowledge(nyx, democrawler, true));
        knowledge.save(new Knowledge(awing, dawn, true));

        mockMvc.perform(get("/api/stats/listeners/" + nyx.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.votes").value(3))
            .andExpect(jsonPath("$.known").value(2))
            .andExpect(jsonPath("$.remaining").value(1));
    }

    @Test
    void refusesAnUnknownListener() throws Exception {
        mockMvc.perform(get("/api/stats/listeners/999999"))
            .andExpect(status().isNotFound())
            .andExpect(status().reason("Pseudo introuvable"));
    }
}
