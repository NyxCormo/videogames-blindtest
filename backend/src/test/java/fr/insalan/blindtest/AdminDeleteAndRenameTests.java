package fr.insalan.blindtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import tools.jackson.databind.ObjectMapper;

import fr.insalan.blindtest.dto.RenameRequest;
import fr.insalan.blindtest.model.Blindtest;
import fr.insalan.blindtest.model.Franchise;
import fr.insalan.blindtest.model.Game;
import fr.insalan.blindtest.model.Listener;
import fr.insalan.blindtest.model.Tag;
import fr.insalan.blindtest.model.TagType;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.model.TrackTag;
import fr.insalan.blindtest.repository.BlindtestRepository;
import fr.insalan.blindtest.repository.FranchiseRepository;
import fr.insalan.blindtest.repository.GameRepository;
import fr.insalan.blindtest.repository.ListenerRepository;
import fr.insalan.blindtest.repository.TagRepository;
import fr.insalan.blindtest.repository.TagTypeRepository;
import fr.insalan.blindtest.repository.TrackRepository;
import fr.insalan.blindtest.repository.TrackTagRepository;
import fr.insalan.blindtest.security.TokenStore;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminDeleteAndRenameTests {

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
    TagTypeRepository tagTypes;

    @Autowired
    TagRepository tags;

    @Autowired
    TrackTagRepository trackTags;

    @Autowired
    ListenerRepository listeners;

    @Autowired
    BlindtestRepository blindtests;

    @AfterEach
    void cleanDatabase() {
        // tag_type est une liste fixe (migration V3), on ne la nettoie pas entre les tests
        trackTags.deleteAll();
        tags.deleteAll();
        tracks.deleteAll();
        games.deleteAll();
        franchises.deleteAll();
        listeners.deleteAll();
        blindtests.deleteAll();
    }

    @Test
    void previewsRequireAnAdminToken() throws Exception {
        Franchise stellar = franchises.save(new Franchise("Stellar Blade"));

        mockMvc.perform(get("/api/admin/franchises/" + stellar.getId() + "/delete-preview"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void refusesToDeleteAFranchiseThatStillHasGames() throws Exception {
        Franchise stellar = franchises.save(new Franchise("Stellar Blade"));
        Game game = games.save(new Game("Stellar Blade", stellar));
        tracks.save(new Track("Dawn", game));

        admin(get("/api/admin/franchises/" + stellar.getId() + "/delete-preview"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.allowed").value(false))
            .andExpect(jsonPath("$.impact").value("« Stellar Blade » contient 1 jeu et 1 musique : la fusionner ou la vider d'abord."));

        admin(delete("/api/admin/franchises/" + stellar.getId()))
            .andExpect(status().isConflict());
        assertThat(franchises.existsById(stellar.getId())).isTrue();
    }

    @Test
    void deletesAnEmptyFranchise() throws Exception {
        Franchise empty = franchises.save(new Franchise("Stellar Blade"));

        admin(get("/api/admin/franchises/" + empty.getId() + "/delete-preview"))
            .andExpect(jsonPath("$.allowed").value(true));
        admin(delete("/api/admin/franchises/" + empty.getId()))
            .andExpect(status().isNoContent());

        assertThat(franchises.existsById(empty.getId())).isFalse();
    }

    @Test
    void refusesToDeleteAGameWithTracksButDeletesAnEmptyOne() throws Exception {
        Franchise stellar = franchises.save(new Franchise("Stellar Blade"));
        Game full = games.save(new Game("Stellar Blade", stellar));
        tracks.save(new Track("Dawn", full));
        Game empty = games.save(new Game("Stellar Blade 2", stellar));

        admin(delete("/api/admin/games/" + full.getId()))
            .andExpect(status().isConflict())
            .andExpect(status().reason("« Stellar Blade » contient 1 musique : le fusionner ou le vider d'abord."));
        admin(delete("/api/admin/games/" + empty.getId()))
            .andExpect(status().isNoContent());

        assertThat(games.existsById(full.getId())).isTrue();
        assertThat(games.existsById(empty.getId())).isFalse();
    }

    @Test
    void previewOfAnUnusedTrackMentionsItsTags() throws Exception {
        Track dawn = tracks.save(new Track("Dawn", games.save(new Game("Stellar Blade", franchises.save(new Franchise("Stellar Blade"))))));
        TagType genre = tagTypes.findByName("genre").orElseThrow();
        trackTags.save(new TrackTag(dawn, tags.save(new Tag("Action", genre))));

        admin(get("/api/admin/tracks/" + dawn.getId() + "/delete-preview"))
            .andExpect(jsonPath("$.allowed").value(true))
            .andExpect(jsonPath("$.impact").value("« Dawn » n'a ni vote ni blindtest. Son tag lui sera retiré."));
    }

    @Test
    void previewOfAnUnknownElementIsNotFound() throws Exception {
        admin(get("/api/admin/games/999999/delete-preview"))
            .andExpect(status().isNotFound());
    }

    @Test
    void renamesAListenerButRefusesANameAlreadyTaken() throws Exception {
        Listener nyx = listeners.save(new Listener("nyx"));
        listeners.save(new Listener("Awing"));

        rename("/api/admin/listeners/" + nyx.getId(), "Nyx")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Nyx"));
        rename("/api/admin/listeners/" + nyx.getId(), "awing")
            .andExpect(status().isConflict());
    }

    @Test
    void renamesABlindtestButRefusesABlankName() throws Exception {
        Blindtest blindtest = blindtests.save(new Blindtest("dazd", 50, 3));

        rename("/api/admin/blindtests/" + blindtest.getId(), "  Soirée Stellar Blade ")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Soirée Stellar Blade"));
        rename("/api/admin/blindtests/" + blindtest.getId(), " ")
            .andExpect(status().isBadRequest());
    }

    private ResultActions admin(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header("Authorization", "Bearer " + tokenStore.create("admin")));
    }

    private ResultActions rename(String url, String name) throws Exception {
        return admin(patch(url)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new RenameRequest(name))));
    }
}
