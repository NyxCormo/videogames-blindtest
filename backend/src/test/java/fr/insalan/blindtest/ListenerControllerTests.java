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

import fr.insalan.blindtest.dto.CreateListenerRequest;
import fr.insalan.blindtest.model.Listener;
import fr.insalan.blindtest.repository.ListenerRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ListenerControllerTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ListenerRepository listeners;

    @AfterEach
    void cleanDatabase() {
        listeners.deleteAll();
    }

    @Test
    void searchFindsApproximateMatchIgnoringCase() throws Exception {
        listeners.save(new Listener("NyxCormo"));
        listeners.save(new Listener("Clément"));

        mockMvc.perform(get("/api/listeners").param("search", "nyx"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("NyxCormo"));
    }

    @Test
    void createsANewListener() throws Exception {
        CreateListenerRequest request = new CreateListenerRequest("Nyx");

        mockMvc.perform(post("/api/listeners")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Nyx"));

        mockMvc.perform(get("/api/listeners").param("search", "Nyx"))
            .andExpect(jsonPath("$[0].name").value("Nyx"));
    }

    @Test
    void rejectsBlankName() throws Exception {
        CreateListenerRequest request = new CreateListenerRequest(" ");

        mockMvc.perform(post("/api/listeners")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}
