package fr.insalan.blindtest;

import static org.assertj.core.api.Assertions.assertThat;
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

import fr.insalan.blindtest.dto.CreateFranchiseRequest;
import fr.insalan.blindtest.model.Franchise;
import fr.insalan.blindtest.model.Game;
import fr.insalan.blindtest.repository.FranchiseRepository;
import fr.insalan.blindtest.repository.GameRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FranchiseControllerTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    FranchiseRepository franchises;

    @Autowired
    GameRepository games;

    @AfterEach
    void cleanDatabase() {
        games.deleteAll();
        franchises.deleteAll();
    }

    @Test
    void listIncludesFranchisesWithoutAnyGame() throws Exception {
        Franchise stellarBlade = franchises.save(new Franchise("Stellar Blade"));
        games.save(new Game("Stellar Blade", stellarBlade));
        franchises.save(new Franchise("Franchise sans jeu"));

        mockMvc.perform(get("/api/franchises"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].name").value("Franchise sans jeu"))
            .andExpect(jsonPath("$[1].name").value("Stellar Blade"));
    }

    @Test
    void createsANewFranchise() throws Exception {
        CreateFranchiseRequest request = new CreateFranchiseRequest("Nouvelle franchise");

        mockMvc.perform(post("/api/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Nouvelle franchise"));

        assertThat(franchises.findByName("Nouvelle franchise")).isPresent();
    }

    @Test
    void reusesAnExistingFranchiseWithTheSameName() throws Exception {
        Franchise existing = franchises.save(new Franchise("Stellar Blade"));
        CreateFranchiseRequest request = new CreateFranchiseRequest("Stellar Blade");

        mockMvc.perform(post("/api/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(existing.getId()));

        assertThat(franchises.count()).isEqualTo(1);
    }

    @Test
    void rejectsABlankName() throws Exception {
        CreateFranchiseRequest request = new CreateFranchiseRequest("  ");

        mockMvc.perform(post("/api/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}
