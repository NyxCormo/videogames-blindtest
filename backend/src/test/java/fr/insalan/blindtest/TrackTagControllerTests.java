package fr.insalan.blindtest;

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

import fr.insalan.blindtest.dto.AddTagRequest;
import fr.insalan.blindtest.model.Franchise;
import fr.insalan.blindtest.model.Game;
import fr.insalan.blindtest.model.Tag;
import fr.insalan.blindtest.model.TagType;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.repository.FranchiseRepository;
import fr.insalan.blindtest.repository.GameRepository;
import fr.insalan.blindtest.repository.TagRepository;
import fr.insalan.blindtest.repository.TagTypeRepository;
import fr.insalan.blindtest.repository.TrackRepository;
import fr.insalan.blindtest.repository.TrackTagRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TrackTagControllerTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    TrackTagRepository trackTags;

    @Autowired
    TagRepository tags;

    @Autowired
    TagTypeRepository tagTypes;

    @Autowired
    TrackRepository tracks;

    @Autowired
    GameRepository games;

    @Autowired
    FranchiseRepository franchises;

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
    void addsListsAndRemovesATag() throws Exception {
        Franchise franchise = franchises.save(new Franchise("Stellar Blade"));
        Game game = games.save(new Game("Stellar Blade", franchise));
        Track dawn = tracks.save(new Track("Dawn", game));
        TagType genre = tagTypes.findByName("genre").orElseThrow();
        Tag action = tags.save(new Tag("Action", genre));

        mockMvc.perform(post("/api/tracks/" + dawn.getId() + "/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddTagRequest(action.getId()))))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tracks/" + dawn.getId() + "/tags"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Action"))
            .andExpect(jsonPath("$[0].typeName").value("genre"));

        mockMvc.perform(delete("/api/tracks/" + dawn.getId() + "/tags/" + action.getId()))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tracks/" + dawn.getId() + "/tags"))
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void addingTheSameTagTwiceDoesNothingExtra() throws Exception {
        Franchise franchise = franchises.save(new Franchise("Stellar Blade"));
        Game game = games.save(new Game("Stellar Blade", franchise));
        Track dawn = tracks.save(new Track("Dawn", game));
        TagType genre = tagTypes.findByName("genre").orElseThrow();
        Tag action = tags.save(new Tag("Action", genre));
        AddTagRequest request = new AddTagRequest(action.getId());

        mockMvc.perform(post("/api/tracks/" + dawn.getId() + "/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());
        mockMvc.perform(post("/api/tracks/" + dawn.getId() + "/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tracks/" + dawn.getId() + "/tags"))
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void removingAnAbsentTagDoesNotFail() throws Exception {
        Franchise franchise = franchises.save(new Franchise("Stellar Blade"));
        Game game = games.save(new Game("Stellar Blade", franchise));
        Track dawn = tracks.save(new Track("Dawn", game));
        TagType genre = tagTypes.findByName("genre").orElseThrow();
        Tag action = tags.save(new Tag("Action", genre));

        mockMvc.perform(delete("/api/tracks/" + dawn.getId() + "/tags/" + action.getId()))
            .andExpect(status().isNoContent());
    }
}
