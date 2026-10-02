package fr.insalan.blindtest;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.test.web.servlet.ResultActions;

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
import fr.insalan.blindtest.security.TokenStore;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminMergePreviewTests {

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
    KnowledgeRepository knowledge;

    @AfterEach
    void cleanDatabase() {
        knowledge.deleteAll();
        tracks.deleteAll();
        games.deleteAll();
        franchises.deleteAll();
        listeners.deleteAll();
    }

    @Test
    void requiresAnAdminToken() throws Exception {
        Franchise stellar = franchises.save(new Franchise("Stellar Blade"));
        Franchise copy = franchises.save(new Franchise("Stellar blade"));

        mockMvc.perform(get("/api/admin/franchises/" + copy.getId() + "/merge-preview").param("into", stellar.getId().toString()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void previewOfATrackMergeCountsWhatMovesAndSharedVotes() throws Exception {
        Game game = stellarBlade();
        Track dawn = tracks.save(new Track("Dawn", game));
        Track copy = new Track("Dawn (bis)", game);
        copy.setYoutubeLink("https://www.youtube.com/watch?v=dawn");
        copy = tracks.save(copy);
        Listener nyx = listeners.save(new Listener("Nyx"));
        knowledge.save(new Knowledge(nyx, dawn, true));
        knowledge.save(new Knowledge(nyx, copy, false));
        knowledge.save(new Knowledge(listeners.save(new Listener("Awing")), copy, true));

        preview("tracks", copy.getId(), dawn.getId())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.allowed").value(true))
            .andExpect(jsonPath("$.summary").value("« Dawn (bis) » (Stellar Blade) sera supprimée. « Dawn » (Stellar Blade) récupère 2 votes, 0 tag et 0 blindtest, "
                + "ainsi que son lien YouTube. 1 personne a voté pour les deux : son vote pour « Dawn » est gardé."));
    }

    @Test
    void previewOfAGameMergeIsRefusedWhenATrackNameIsShared() throws Exception {
        Franchise stellar = franchises.save(new Franchise("Stellar Blade"));
        Game game = games.save(new Game("Stellar Blade", stellar));
        Game copy = games.save(new Game("Stellar blade", stellar));
        tracks.save(new Track("Dawn", game));
        tracks.save(new Track("dawn", copy));

        preview("games", copy.getId(), game.getId())
            .andExpect(jsonPath("$.allowed").value(false))
            .andExpect(jsonPath("$.summary").value("Musiques présentes dans les deux jeux, à fusionner d'abord : dawn"));
    }

    @Test
    void previewOfAFranchiseMergeCountsGamesAndTracks() throws Exception {
        Franchise stellar = franchises.save(new Franchise("Stellar Blade"));
        Franchise copy = franchises.save(new Franchise("Stellar blade"));
        Game game = games.save(new Game("Stellar Blade 2", copy));
        tracks.save(new Track("Dawn", game));
        tracks.save(new Track("Raven", game));

        preview("franchises", copy.getId(), stellar.getId())
            .andExpect(jsonPath("$.allowed").value(true))
            .andExpect(jsonPath("$.summary").value("« Stellar blade » sera supprimée et son contenu (1 jeu, 2 musiques) passera dans « Stellar Blade »."));
    }

    @Test
    void refusesToMergeAnElementWithItself() throws Exception {
        Franchise stellar = franchises.save(new Franchise("Stellar Blade"));

        preview("franchises", stellar.getId(), stellar.getId())
            .andExpect(status().isBadRequest());
    }

    @Test
    void refusesATargetOfAnotherType() throws Exception {
        Franchise stellar = franchises.save(new Franchise("Stellar Blade"));
        Game game = games.save(new Game("Stellar Blade", stellar));
        Franchise other = franchises.save(new Franchise("Autre"));
        // Le type vient de l'adresse : l'id d'une franchise n'est pas un jeu.
        assertThat(games.existsById(other.getId())).isFalse();

        preview("games", game.getId(), other.getId())
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Jeu cible inconnu"));
    }

    @Test
    void anUnknownSourceIsNotFound() throws Exception {
        Franchise stellar = franchises.save(new Franchise("Stellar Blade"));

        preview("franchises", 999999, stellar.getId())
            .andExpect(status().isNotFound());
    }

    private Game stellarBlade() {
        return games.save(new Game("Stellar Blade", franchises.save(new Franchise("Stellar Blade"))));
    }

    private ResultActions preview(String kind, Integer sourceId, Integer targetId) throws Exception {
        return mockMvc.perform(get("/api/admin/" + kind + "/" + sourceId + "/merge-preview")
            .param("into", targetId.toString())
            .header("Authorization", "Bearer " + tokenStore.create("admin")));
    }
}
