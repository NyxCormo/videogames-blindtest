package fr.insalan.blindtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import tools.jackson.databind.ObjectMapper;

import fr.insalan.blindtest.dto.CreateListenerRequest;
import fr.insalan.blindtest.model.Listener;
import fr.insalan.blindtest.repository.ListenerRepository;

class ListenerControllerTests extends IntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ListenerRepository listeners;

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
    void rejectsANameThatAlreadyExistsIgnoringCase() throws Exception {
        listeners.save(new Listener("Nyx"));
        CreateListenerRequest request = new CreateListenerRequest("nyx");

        mockMvc.perform(post("/api/listeners")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());

        assertThat(listeners.count()).isEqualTo(1);
    }

    @Test
    void createdListenerKnowsItsDates() throws Exception {
        Instant before = Instant.now();

        mockMvc.perform(post("/api/listeners")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateListenerRequest("Nyx"))))
            .andExpect(status().isCreated());

        Listener nyx = listeners.findByName("Nyx").orElseThrow();
        assertThat(nyx.getCreatedAt()).isAfterOrEqualTo(before);
        assertThat(nyx.getLastSeenAt()).isEqualTo(nyx.getCreatedAt());
    }

    @Test
    void openingASessionReturnsTheCurrentNameAndRecordsTheVisit() throws Exception {
        Listener nyx = listeners.save(new Listener("Nyx"));
        Instant firstSeen = nyx.getLastSeenAt();
        nyx.setName("NyxCormo");
        listeners.save(nyx);

        mockMvc.perform(post("/api/listeners/" + nyx.getId() + "/session"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(nyx.getId()))
            .andExpect(jsonPath("$.name").value("NyxCormo"));

        assertThat(listeners.findById(nyx.getId()).orElseThrow().getLastSeenAt()).isAfterOrEqualTo(firstSeen);
    }

    @Test
    void openingASessionForAnUnknownListenerIsRefused() throws Exception {
        mockMvc.perform(post("/api/listeners/999999/session"))
            .andExpect(status().isNotFound())
            .andExpect(status().reason("Pseudo introuvable"));
    }

    @Test
    void searchIgnoresAccentsAndPunctuation() throws Exception {
        listeners.save(new Listener("Kévin.lny"));

        mockMvc.perform(get("/api/listeners").param("search", "kevin lny"))
            .andExpect(jsonPath("$[0].name").value("Kévin.lny"));
    }

    @Test
    void searchPutsTheExactPseudoFirst() throws Exception {
        listeners.save(new Listener("ANyx"));
        listeners.save(new Listener("Nyxie"));
        listeners.save(new Listener("Nyx"));

        mockMvc.perform(get("/api/listeners").param("search", "nyx"))
            .andExpect(jsonPath("$[0].name").value("Nyx"))
            .andExpect(jsonPath("$[1].name").value("Nyxie"))
            .andExpect(jsonPath("$[2].name").value("ANyx"));
    }

    @Test
    void findsSimilarPseudos() throws Exception {
        listeners.save(new Listener("Nyx"));
        listeners.save(new Listener("SkytAsul"));
        listeners.save(new Listener("Awing"));

        mockMvc.perform(get("/api/listeners/similar").param("name", "Nix"))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("Nyx"));
        mockMvc.perform(get("/api/listeners/similar").param("name", "Skyt Asul 2"))
            .andExpect(jsonPath("$[0].name").value("SkytAsul"));
        mockMvc.perform(get("/api/listeners/similar").param("name", "Raven"))
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void lookingForSimilarPseudosChecksTheRulesFirst() throws Exception {
        mockMvc.perform(get("/api/listeners/similar").param("name", "Gros con"))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Ce pseudo n'est pas accepté"));
        mockMvc.perform(get("/api/listeners/similar").param("name", "N"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void cleansSpacesBeforeSaving() throws Exception {
        create("  Stellar   Blade  ")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Stellar Blade"));
    }

    @Test
    void refusesAPseudoThatOnlyDiffersByAccentsOrPunctuation() throws Exception {
        listeners.save(new Listener("SkytAsul"));

        create("Skyt-Asul")
            .andExpect(status().isConflict())
            .andExpect(status().reason("Le pseudo « Skyt-Asul » existe déjà sous la forme « SkytAsul »"));
        create("skytäsul").andExpect(status().isConflict());
        assertThat(listeners.count()).isEqualTo(1);
    }

    @Test
    void refusesPseudosOutsideTheRules() throws Exception {
        create("N").andExpect(status().isBadRequest());
        create("Un pseudo beaucoup trop long").andExpect(status().isBadRequest());
        create("Nyx 🎵").andExpect(status().isBadRequest());
        create("-.-").andExpect(status().isBadRequest());
        create("Gros con").andExpect(status().isBadRequest()).andExpect(status().reason("Ce pseudo n'est pas accepté"));
        assertThat(listeners.count()).isZero();
    }

    @Test
    void acceptsAWordThatOnlyContainsARefusedOne() throws Exception {
        create("Conan").andExpect(status().isCreated());
    }

    @Test
    void rejectsBlankName() throws Exception {
        CreateListenerRequest request = new CreateListenerRequest(" ");

        mockMvc.perform(post("/api/listeners")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    private ResultActions create(String name) throws Exception {
        return mockMvc.perform(post("/api/listeners")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new CreateListenerRequest(name))));
    }
}
