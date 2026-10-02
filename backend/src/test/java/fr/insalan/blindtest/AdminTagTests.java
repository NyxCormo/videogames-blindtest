package fr.insalan.blindtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import tools.jackson.databind.ObjectMapper;

import fr.insalan.blindtest.dto.MergeRequest;
import fr.insalan.blindtest.dto.RenameRequest;
import fr.insalan.blindtest.model.Franchise;
import fr.insalan.blindtest.model.Game;
import fr.insalan.blindtest.model.Tag;
import fr.insalan.blindtest.model.TagType;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.model.TrackTag;
import fr.insalan.blindtest.repository.FranchiseRepository;
import fr.insalan.blindtest.repository.GameRepository;
import fr.insalan.blindtest.repository.TagRepository;
import fr.insalan.blindtest.repository.TagTypeRepository;
import fr.insalan.blindtest.repository.TrackRepository;
import fr.insalan.blindtest.repository.TrackTagRepository;
import fr.insalan.blindtest.security.TokenStore;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminTagTests {

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

    @AfterEach
    void cleanDatabase() {
        // tag_type est une liste fixe (migration V3), on ne la nettoie pas entre les tests
        trackTags.deleteAll();
        tags.deleteAll();
        tracks.deleteAll();
        games.deleteAll();
        franchises.deleteAll();
    }

    @Test
    void requiresAnAdminToken() throws Exception {
        mockMvc.perform(get("/api/admin/tags"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void listsTagsWithTheirNumberOfTracks() throws Exception {
        TagType genre = tagTypes.findByName("genre").orElseThrow();
        Tag action = tags.save(new Tag("Action", genre));
        tags.save(new Tag("RPG", genre));
        trackTags.save(new TrackTag(tracks.save(new Track("Dawn", stellarBlade())), action));

        mockMvc.perform(get("/api/admin/tags").header("Authorization", adminToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Action"))
            .andExpect(jsonPath("$[0].typeName").value("genre"))
            .andExpect(jsonPath("$[0].tracks").value(1))
            .andExpect(jsonPath("$[1].name").value("RPG"))
            .andExpect(jsonPath("$[1].tracks").value(0));
    }

    @Test
    void renamesATagButRefusesANameAlreadyUsedInTheSameType() throws Exception {
        TagType genre = tagTypes.findByName("genre").orElseThrow();
        TagType ambiance = tagTypes.findByName("ambiance").orElseThrow();
        Tag typo = tags.save(new Tag("Acton", genre));
        tags.save(new Tag("RPG", genre));
        tags.save(new Tag("Calme", ambiance));

        send(patch("/api/admin/tags/" + typo.getId()), new RenameRequest("Action"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Action"))
            .andExpect(jsonPath("$.typeName").value("genre"));

        send(patch("/api/admin/tags/" + typo.getId()), new RenameRequest("rpg"))
            .andExpect(status().isConflict());

        send(patch("/api/admin/tags/" + typo.getId()), new RenameRequest("Calme"))
            .andExpect(status().isOk());
    }

    @Test
    void mergingTagsMovesThemWithoutDuplicates() throws Exception {
        TagType genre = tagTypes.findByName("genre").orElseThrow();
        Tag action = tags.save(new Tag("Action", genre));
        Tag typo = tags.save(new Tag("Acton", genre));
        Game game = stellarBlade();
        Track dawn = tracks.save(new Track("Dawn", game));
        Track raven = tracks.save(new Track("Raven", game));
        trackTags.save(new TrackTag(dawn, action));
        trackTags.save(new TrackTag(dawn, typo));
        trackTags.save(new TrackTag(raven, typo));

        send(post("/api/admin/tags/" + typo.getId() + "/merge"), new MergeRequest(action.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Action"));

        assertThat(tags.existsById(typo.getId())).isFalse();
        assertThat(trackTags.findWithTagByTrackId(dawn.getId())).hasSize(1);
        assertThat(trackTags.findWithTagByTrackId(raven.getId()))
            .singleElement()
            .satisfies(trackTag -> assertThat(trackTag.getTag().getId()).isEqualTo(action.getId()));
    }

    @Test
    void deletingATagRemovesItFromItsTracks() throws Exception {
        TagType genre = tagTypes.findByName("genre").orElseThrow();
        Tag action = tags.save(new Tag("Action", genre));
        Track dawn = tracks.save(new Track("Dawn", stellarBlade()));
        trackTags.save(new TrackTag(dawn, action));

        mockMvc.perform(delete("/api/admin/tags/" + action.getId()).header("Authorization", adminToken()))
            .andExpect(status().isNoContent());

        assertThat(tags.existsById(action.getId())).isFalse();
        assertThat(trackTags.count()).isZero();
        assertThat(tracks.existsById(dawn.getId())).isTrue();
    }

    private Game stellarBlade() {
        return games.save(new Game("Stellar Blade", franchises.save(new Franchise("Stellar Blade"))));
    }

    private String adminToken() {
        return "Bearer " + tokenStore.create("admin");
    }

    private ResultActions send(MockHttpServletRequestBuilder request, Object body) throws Exception {
        return mockMvc.perform(request
            .header("Authorization", adminToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)));
    }
}
