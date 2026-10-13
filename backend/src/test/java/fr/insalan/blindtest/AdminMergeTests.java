package fr.insalan.blindtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import tools.jackson.databind.ObjectMapper;

import fr.insalan.blindtest.dto.admin.MergeRequest;
import fr.insalan.blindtest.model.Blindtest;
import fr.insalan.blindtest.model.BlindtestTrack;
import fr.insalan.blindtest.model.Franchise;
import fr.insalan.blindtest.model.Game;
import fr.insalan.blindtest.model.Knowledge;
import fr.insalan.blindtest.model.Listener;
import fr.insalan.blindtest.model.Tag;
import fr.insalan.blindtest.model.TagType;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.model.TrackReport;
import fr.insalan.blindtest.model.TrackReportId;
import fr.insalan.blindtest.model.TrackTag;
import fr.insalan.blindtest.repository.BlindtestRepository;
import fr.insalan.blindtest.repository.BlindtestTrackRepository;
import fr.insalan.blindtest.repository.FranchiseRepository;
import fr.insalan.blindtest.repository.GameRepository;
import fr.insalan.blindtest.repository.KnowledgeRepository;
import fr.insalan.blindtest.repository.ListenerRepository;
import fr.insalan.blindtest.repository.TagRepository;
import fr.insalan.blindtest.repository.TagTypeRepository;
import fr.insalan.blindtest.repository.TrackReportRepository;
import fr.insalan.blindtest.repository.TrackRepository;
import fr.insalan.blindtest.repository.TrackTagRepository;

class AdminMergeTests extends IntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

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
    TrackReportRepository reports;

    @Test
    void requiresAnAdminToken() throws Exception {
        Game game = stellarBlade();
        Track dawn = tracks.save(new Track("Dawn", game));
        Track copy = tracks.save(new Track("Dawn (bis)", game));

        mockMvc.perform(post("/api/admin/tracks/" + copy.getId() + "/merge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new MergeRequest(dawn.getId()))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void refusesToMergeATrackWithItself() throws Exception {
        Track dawn = tracks.save(new Track("Dawn", stellarBlade()));

        merge("/api/admin/tracks/" + dawn.getId() + "/merge", dawn.getId())
            .andExpect(status().isBadRequest());
    }

    @Test
    void mergingTracksMovesVotesTagsLinksAndBlindtests() throws Exception {
        Game game = stellarBlade();
        Track dawn = tracks.save(new Track("Dawn", game));
        Track copy = new Track("Dawn (bis)", game);
        copy.setYoutubeLink("https://www.youtube.com/watch?v=dawn");
        copy = tracks.save(copy);

        Listener nyx = listeners.save(new Listener("Nyx"));
        Listener awing = listeners.save(new Listener("Awing"));
        knowledge.save(new Knowledge(nyx, dawn, true));
        knowledge.save(new Knowledge(nyx, copy, false));
        knowledge.save(new Knowledge(awing, copy, true));

        TagType genre = tagTypes.findByName("genre").orElseThrow();
        Tag action = tags.save(new Tag("Action", genre));
        Tag rpg = tags.save(new Tag("RPG", genre));
        trackTags.save(new TrackTag(dawn, action));
        trackTags.save(new TrackTag(copy, action));
        trackTags.save(new TrackTag(copy, rpg));

        Blindtest blindtest = blindtests.save(new Blindtest("Soirée", 50, 3));
        blindtestTracks.save(new BlindtestTrack(blindtest, copy, 0));

        merge("/api/admin/tracks/" + copy.getId() + "/merge", dawn.getId())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Dawn"))
            .andExpect(jsonPath("$.youtubeLink").value("https://www.youtube.com/watch?v=dawn"));

        assertThat(tracks.existsById(copy.getId())).isFalse();
        assertThat(knowledge.findByListenerId(nyx.getId()))
            .singleElement()
            .satisfies(vote -> assertThat(vote.isKnows()).isTrue());
        assertThat(knowledge.findByListenerId(awing.getId()))
            .singleElement()
            .satisfies(vote -> assertThat(vote.getTrack().getId()).isEqualTo(dawn.getId()));
        assertThat(trackTags.findWithTagByTrackId(dawn.getId())).hasSize(2);
        assertThat(blindtestTracks.findAll())
            .singleElement()
            .satisfies(entry -> assertThat(entry.getTrack().getId()).isEqualTo(dawn.getId()));
    }

    @Test
    void mergingTracksMovesReportsWithoutDuplicates() throws Exception {
        Game game = stellarBlade();
        Track dawn = tracks.save(new Track("Dawn", game));
        Track copy = tracks.save(new Track("Dawn (bis)", game));
        Listener nyx = listeners.save(new Listener("Nyx"));
        Listener awing = listeners.save(new Listener("Awing"));
        reports.save(new TrackReport(dawn, nyx));
        reports.save(new TrackReport(copy, nyx));
        reports.save(new TrackReport(copy, awing));

        merge("/api/admin/tracks/" + copy.getId() + "/merge", dawn.getId())
            .andExpect(status().isOk());

        assertThat(reports.count()).isEqualTo(2);
        assertThat(reports.existsById(new TrackReportId(dawn.getId(), nyx.getId()))).isTrue();
        assertThat(reports.existsById(new TrackReportId(dawn.getId(), awing.getId()))).isTrue();
    }

    @Test
    void mergingGamesMovesTheirTracks() throws Exception {
        Franchise stellar = franchises.save(new Franchise("Stellar Blade"));
        Game game = games.save(new Game("Stellar Blade", stellar));
        Game copy = games.save(new Game("Stellar blade", stellar));
        tracks.save(new Track("Dawn", game));
        Track raven = tracks.save(new Track("Raven", copy));

        merge("/api/admin/games/" + copy.getId() + "/merge", game.getId())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Stellar Blade"));

        assertThat(games.existsById(copy.getId())).isFalse();
        assertThat(tracks.findByIdWithGameAndFranchise(raven.getId()).orElseThrow().getGame().getId()).isEqualTo(game.getId());
    }

    @Test
    void refusesToMergeGamesThatShareATrackName() throws Exception {
        Franchise stellar = franchises.save(new Franchise("Stellar Blade"));
        Game game = games.save(new Game("Stellar Blade", stellar));
        Game copy = games.save(new Game("Stellar blade", stellar));
        tracks.save(new Track("Dawn", game));
        tracks.save(new Track("dawn", copy));

        merge("/api/admin/games/" + copy.getId() + "/merge", game.getId())
            .andExpect(status().isConflict())
            .andExpect(status().reason("Musiques présentes dans les deux jeux, à fusionner d'abord : dawn"));

        assertThat(games.existsById(copy.getId())).isTrue();
    }

    @Test
    void mergingFranchisesMovesTheirGames() throws Exception {
        Franchise stellar = franchises.save(new Franchise("Stellar Blade"));
        Franchise copy = franchises.save(new Franchise("Stellar blade"));
        Game game = games.save(new Game("Stellar Blade", copy));

        merge("/api/admin/franchises/" + copy.getId() + "/merge", stellar.getId())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Stellar Blade"));

        assertThat(franchises.existsById(copy.getId())).isFalse();
        assertThat(games.findByIdWithFranchise(game.getId()).orElseThrow().getFranchise().getId()).isEqualTo(stellar.getId());
    }

    @Test
    void refusesToMergeFranchisesThatShareAGameName() throws Exception {
        Franchise stellar = franchises.save(new Franchise("Stellar Blade"));
        Franchise copy = franchises.save(new Franchise("Stellar blade"));
        games.save(new Game("Stellar Blade", stellar));
        games.save(new Game("Stellar Blade", copy));

        merge("/api/admin/franchises/" + copy.getId() + "/merge", stellar.getId())
            .andExpect(status().isConflict());

        assertThat(franchises.existsById(copy.getId())).isTrue();
    }

    private ResultActions merge(String url, Integer targetId) throws Exception {
        return mockMvc.perform(post(url)
            .header("Authorization", adminToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new MergeRequest(targetId))));
    }
}
