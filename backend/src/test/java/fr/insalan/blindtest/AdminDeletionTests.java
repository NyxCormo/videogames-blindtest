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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

import fr.insalan.blindtest.dto.MergeRequest;
import fr.insalan.blindtest.model.Blindtest;
import fr.insalan.blindtest.model.BlindtestDifficultyBand;
import fr.insalan.blindtest.model.BlindtestScore;
import fr.insalan.blindtest.model.BlindtestTrack;
import fr.insalan.blindtest.model.Franchise;
import fr.insalan.blindtest.model.Game;
import fr.insalan.blindtest.model.Knowledge;
import fr.insalan.blindtest.model.Listener;
import fr.insalan.blindtest.model.Tag;
import fr.insalan.blindtest.model.TagType;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.model.TrackTag;
import fr.insalan.blindtest.repository.BlindtestDifficultyBandRepository;
import fr.insalan.blindtest.repository.BlindtestRepository;
import fr.insalan.blindtest.repository.BlindtestScoreRepository;
import fr.insalan.blindtest.repository.BlindtestTrackRepository;
import fr.insalan.blindtest.repository.FranchiseRepository;
import fr.insalan.blindtest.repository.GameRepository;
import fr.insalan.blindtest.repository.KnowledgeRepository;
import fr.insalan.blindtest.repository.ListenerRepository;
import fr.insalan.blindtest.repository.TagRepository;
import fr.insalan.blindtest.repository.TagTypeRepository;
import fr.insalan.blindtest.repository.TrackRepository;
import fr.insalan.blindtest.repository.TrackTagRepository;
import fr.insalan.blindtest.security.TokenStore;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminDeletionTests {

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

    @Autowired
    ListenerRepository listeners;

    @Autowired
    KnowledgeRepository knowledge;

    @Autowired
    TagTypeRepository tagTypes;

    @Autowired
    TagRepository tags;

    @Autowired
    TrackTagRepository trackTags;

    @Autowired
    BlindtestRepository blindtests;

    @Autowired
    BlindtestTrackRepository blindtestTracks;

    @Autowired
    BlindtestScoreRepository blindtestScores;

    @Autowired
    BlindtestDifficultyBandRepository blindtestBands;

    @AfterEach
    void cleanDatabase() {
        // tag_type est une liste fixe (migration V3), on ne la nettoie pas entre les tests
        blindtestBands.deleteAll();
        blindtestScores.deleteAll();
        blindtestTracks.deleteAll();
        blindtests.deleteAll();
        knowledge.deleteAll();
        trackTags.deleteAll();
        tags.deleteAll();
        tracks.deleteAll();
        games.deleteAll();
        franchises.deleteAll();
        listeners.deleteAll();
    }

    @Test
    void requiresAnAdminToken() throws Exception {
        Listener nyx = listeners.save(new Listener("Nyx"));

        mockMvc.perform(delete("/api/admin/listeners/" + nyx.getId()))
            .andExpect(status().isUnauthorized());
        assertThat(listeners.existsById(nyx.getId())).isTrue();
    }

    @Test
    void deletesAnUnusedTrackWithItsTags() throws Exception {
        Track dawn = tracks.save(new Track("Dawn", stellarBlade()));
        TagType genre = tagTypes.findByName("genre").orElseThrow();
        trackTags.save(new TrackTag(dawn, tags.save(new Tag("Action", genre))));

        mockMvc.perform(delete("/api/admin/tracks/" + dawn.getId()).header("Authorization", adminToken()))
            .andExpect(status().isNoContent());

        assertThat(tracks.existsById(dawn.getId())).isFalse();
        assertThat(trackTags.count()).isZero();
    }

    @Test
    void refusesToDeleteATrackThatHasVotes() throws Exception {
        Track dawn = tracks.save(new Track("Dawn", stellarBlade()));
        knowledge.save(new Knowledge(listeners.save(new Listener("Nyx")), dawn, true));

        mockMvc.perform(delete("/api/admin/tracks/" + dawn.getId()).header("Authorization", adminToken()))
            .andExpect(status().isConflict());

        assertThat(tracks.existsById(dawn.getId())).isTrue();
    }

    @Test
    void deletingABlindtestRemovesItsTracksScoresAndBandsButNotTheTracksThemselves() throws Exception {
        Track dawn = tracks.save(new Track("Dawn", stellarBlade()));
        Blindtest blindtest = blindtests.save(new Blindtest("Test", 50, 3));
        blindtestTracks.save(new BlindtestTrack(blindtest, dawn, 0));
        blindtestScores.save(new BlindtestScore(blindtest, listeners.save(new Listener("Nyx"))));
        blindtestBands.save(new BlindtestDifficultyBand(blindtest, 0, 0, 100, 100));

        mockMvc.perform(delete("/api/admin/blindtests/" + blindtest.getId()).header("Authorization", adminToken()))
            .andExpect(status().isNoContent());

        assertThat(blindtests.count()).isZero();
        assertThat(blindtestTracks.count()).isZero();
        assertThat(blindtestScores.count()).isZero();
        assertThat(blindtestBands.count()).isZero();
        assertThat(tracks.existsById(dawn.getId())).isTrue();
    }

    @Test
    void deletingAListenerRemovesItsVotesAndScores() throws Exception {
        Track dawn = tracks.save(new Track("Dawn", stellarBlade()));
        Listener nyx = listeners.save(new Listener("Nyx"));
        knowledge.save(new Knowledge(nyx, dawn, true));
        blindtestScores.save(new BlindtestScore(blindtests.save(new Blindtest("Test", 50, 3)), nyx));

        mockMvc.perform(delete("/api/admin/listeners/" + nyx.getId()).header("Authorization", adminToken()))
            .andExpect(status().isNoContent());

        assertThat(listeners.existsById(nyx.getId())).isFalse();
        assertThat(knowledge.count()).isZero();
        assertThat(blindtestScores.count()).isZero();
    }

    @Test
    void listsListenersWithTheirUsage() throws Exception {
        Track dawn = tracks.save(new Track("Dawn", stellarBlade()));
        Listener nyx = listeners.save(new Listener("Nyx"));
        listeners.save(new Listener("Awing"));
        knowledge.save(new Knowledge(nyx, dawn, true));

        mockMvc.perform(get("/api/admin/listeners").header("Authorization", adminToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Awing"))
            .andExpect(jsonPath("$[0].votes").value(0))
            .andExpect(jsonPath("$[1].name").value("Nyx"))
            .andExpect(jsonPath("$[1].votes").value(1))
            .andExpect(jsonPath("$[1].blindtests").value(0));
    }

    @Test
    void mergingListenersMovesVotesAndScoresKeepingTheTargetOnConflict() throws Exception {
        Game game = stellarBlade();
        Track dawn = tracks.save(new Track("Dawn", game));
        Track raven = tracks.save(new Track("Raven", game));
        Listener nyx = listeners.save(new Listener("Nyx"));
        Listener copy = listeners.save(new Listener("Nyx2"));
        knowledge.save(new Knowledge(nyx, dawn, true));
        knowledge.save(new Knowledge(copy, dawn, false));
        knowledge.save(new Knowledge(copy, raven, true));

        Blindtest shared = blindtests.save(new Blindtest("Partagé", 50, 3));
        Blindtest onlyCopy = blindtests.save(new Blindtest("Seul", 50, 3));
        BlindtestScore kept = new BlindtestScore(shared, nyx);
        kept.setGoodAnswers(5);
        blindtestScores.save(kept);
        blindtestScores.save(new BlindtestScore(shared, copy));
        blindtestScores.save(new BlindtestScore(onlyCopy, copy));

        mockMvc.perform(post("/api/admin/listeners/" + copy.getId() + "/merge")
                .header("Authorization", adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new MergeRequest(nyx.getId()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Nyx"));

        assertThat(listeners.existsById(copy.getId())).isFalse();
        assertThat(knowledge.findByListenerId(nyx.getId())).hasSize(2)
            .anySatisfy(vote -> {
                assertThat(vote.getTrack().getId()).isEqualTo(dawn.getId());
                assertThat(vote.isKnows()).isTrue();
            });
        assertThat(blindtestScores.findWithListenerByBlindtestId(shared.getId()))
            .singleElement()
            .satisfies(score -> assertThat(score.getGoodAnswers()).isEqualTo(5));
        assertThat(blindtestScores.findWithListenerByBlindtestId(onlyCopy.getId()))
            .singleElement()
            .satisfies(score -> assertThat(score.getListener().getId()).isEqualTo(nyx.getId()));
    }

    private Game stellarBlade() {
        return games.save(new Game("Stellar Blade", franchises.save(new Franchise("Stellar Blade"))));
    }

    private String adminToken() {
        return "Bearer " + tokenStore.create("admin");
    }
}
