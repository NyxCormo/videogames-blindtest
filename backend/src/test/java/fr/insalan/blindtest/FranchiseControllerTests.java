package fr.insalan.blindtest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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
    FranchiseRepository franchises;

    @Autowired
    GameRepository games;

    @AfterEach
    void cleanDatabase() {
        games.deleteAll();
        franchises.deleteAll();
    }

    @Test
    void listIsEmptyWithoutData() throws Exception {
        mockMvc.perform(get("/api/franchises"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void listsFranchisesSortedByName() throws Exception {
        franchises.save(new Franchise("Stellar Blade"));
        franchises.save(new Franchise("Final Fantasy"));

        mockMvc.perform(get("/api/franchises"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Final Fantasy"))
            .andExpect(jsonPath("$[1].name").value("Stellar Blade"));
    }

    @Test
    void gamesReturnsOnlyGamesOfTheRequestedFranchise() throws Exception {
        Franchise stellarBlade = franchises.save(new Franchise("Stellar Blade"));
        Franchise finalFantasy = franchises.save(new Franchise("Final Fantasy"));
        games.save(new Game("Stellar Blade", stellarBlade));
        games.save(new Game("Final Fantasy VII", finalFantasy));

        mockMvc.perform(get("/api/franchises/" + stellarBlade.getId() + "/games"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("Stellar Blade"));
    }

    @Test
    void gamesAreSortedByName() throws Exception {
        Franchise finalFantasy = franchises.save(new Franchise("Final Fantasy"));
        games.save(new Game("Final Fantasy X", finalFantasy));
        games.save(new Game("Final Fantasy VII", finalFantasy));

        mockMvc.perform(get("/api/franchises/" + finalFantasy.getId() + "/games"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Final Fantasy VII"))
            .andExpect(jsonPath("$[1].name").value("Final Fantasy X"));
    }
}
