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
import fr.insalan.blindtest.model.Tag;
import fr.insalan.blindtest.model.TagType;
import fr.insalan.blindtest.repository.TagRepository;
import fr.insalan.blindtest.repository.TagTypeRepository;

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

    @AfterEach
    void cleanDatabase() {
        tags.deleteAll();
        tagTypes.deleteAll();
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
}
