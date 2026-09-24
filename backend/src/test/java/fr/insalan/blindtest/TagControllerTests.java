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

import tools.jackson.databind.ObjectMapper;

import fr.insalan.blindtest.dto.CreateTagRequest;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TagControllerTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    TagRepository tags;

    @Autowired
    TagTypeRepository tagTypes;

    @Autowired
    TrackTagRepository trackTags;

    @Autowired
    TrackRepository tracks;

    @Autowired
    GameRepository games;

    @Autowired
    FranchiseRepository franchises;

    @AfterEach
    void cleanDatabase() {
        trackTags.deleteAll();
        tags.deleteAll();
        tagTypes.deleteAll();
        tracks.deleteAll();
        games.deleteAll();
        franchises.deleteAll();
    }

    @Test
    void searchFindsExistingTagWithItsType() throws Exception {
        TagType genre = tagTypes.save(new TagType("genre"));
        tags.save(new Tag("Action", genre));

        mockMvc.perform(get("/api/tags").param("search", "act"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Action"))
            .andExpect(jsonPath("$[0].typeName").value("genre"));
    }

    @Test
    void createsTypeAndTagWhenNeitherExists() throws Exception {
        CreateTagRequest request = new CreateTagRequest("ambiance", "Épique");

        mockMvc.perform(post("/api/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Épique"))
            .andExpect(jsonPath("$.typeName").value("ambiance"));

        mockMvc.perform(get("/api/tag-types"))
            .andExpect(jsonPath("$[0].name").value("ambiance"));
    }

    @Test
    void reusesExistingTypeInsteadOfDuplicating() throws Exception {
        tagTypes.save(new TagType("genre"));
        CreateTagRequest request = new CreateTagRequest("genre", "Aventure");

        mockMvc.perform(post("/api/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tag-types"))
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void rejectsBlankTagName() throws Exception {
        CreateTagRequest request = new CreateTagRequest("genre", " ");

        mockMvc.perform(post("/api/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void allReturnsEveryTagRegardlessOfUsage() throws Exception {
        TagType genre = tagTypes.save(new TagType("genre"));
        tags.save(new Tag("Action", genre));
        tags.save(new Tag("Aventure", genre));

        mockMvc.perform(get("/api/tags/all"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void mostUsedIsSortedByNumberOfTracks() throws Exception {
        Franchise franchise = franchises.save(new Franchise("Stellar Blade"));
        Game game = games.save(new Game("Stellar Blade", franchise));
        Track dawn = tracks.save(new Track("Dawn", game));
        Track raven = tracks.save(new Track("Raven", game));
        TagType genre = tagTypes.save(new TagType("genre"));
        Tag action = tags.save(new Tag("Action", genre));
        Tag aventure = tags.save(new Tag("Aventure", genre));
        trackTags.save(new TrackTag(dawn, action));
        trackTags.save(new TrackTag(raven, action));
        trackTags.save(new TrackTag(dawn, aventure));

        mockMvc.perform(get("/api/tags/most-used"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Action"))
            .andExpect(jsonPath("$[0].count").value(2))
            .andExpect(jsonPath("$[1].name").value("Aventure"))
            .andExpect(jsonPath("$[1].count").value(1));
    }
}
