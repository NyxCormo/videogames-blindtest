package fr.insalan.blindtest;

import static org.assertj.core.api.Assertions.assertThat;
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

import tools.jackson.databind.ObjectMapper;

import fr.insalan.blindtest.dto.RenameRequest;
import fr.insalan.blindtest.model.Franchise;
import fr.insalan.blindtest.model.Game;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.repository.FranchiseRepository;
import fr.insalan.blindtest.repository.GameRepository;
import fr.insalan.blindtest.repository.TrackRepository;
import fr.insalan.blindtest.security.TokenStore;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminRenameTests {

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

    @AfterEach
    void cleanDatabase() {
        tracks.deleteAll();
        games.deleteAll();
        franchises.deleteAll();
    }

    @Test
    void requiresAnAdminToken() throws Exception {
        Franchise franchise = franchises.save(new Franchise("Stellar blade"));

        mockMvc.perform(patch("/api/admin/franchises/" + franchise.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RenameRequest("Stellar Blade"))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void renamesAFranchiseEvenWhenOnlyTheCaseChanges() throws Exception {
        Franchise franchise = franchises.save(new Franchise("Stellar blade"));

        rename("/api/admin/franchises/" + franchise.getId(), " Stellar Blade ")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Stellar Blade"));

        assertThat(franchises.findById(franchise.getId()).orElseThrow().getName()).isEqualTo("Stellar Blade");
    }

    @Test
    void refusesANameUsedByAnotherFranchise() throws Exception {
        franchises.save(new Franchise("Stellar Blade"));
        Franchise other = franchises.save(new Franchise("Autre"));

        rename("/api/admin/franchises/" + other.getId(), "stellar blade")
            .andExpect(status().isConflict());
    }

    @Test
    void refusesABlankName() throws Exception {
        Franchise franchise = franchises.save(new Franchise("Stellar Blade"));

        rename("/api/admin/franchises/" + franchise.getId(), "  ")
            .andExpect(status().isBadRequest());
    }

    @Test
    void renamesAGameAndChecksDuplicatesOnlyInItsFranchise() throws Exception {
        Franchise stellar = franchises.save(new Franchise("Stellar Blade"));
        Franchise other = franchises.save(new Franchise("Autre"));
        games.save(new Game("Stellar Blade", other));
        Game game = games.save(new Game("Stellar blade", stellar));

        rename("/api/admin/games/" + game.getId(), "Stellar Blade")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.franchiseName").value("Stellar Blade"));

        Game sameFranchise = games.save(new Game("Autre jeu", stellar));
        rename("/api/admin/games/" + sameFranchise.getId(), "Stellar Blade")
            .andExpect(status().isConflict());
    }

    @Test
    void renamesATrackAndChecksDuplicatesInItsGame() throws Exception {
        Franchise stellar = franchises.save(new Franchise("Stellar Blade"));
        Game game = games.save(new Game("Stellar Blade", stellar));
        tracks.save(new Track("Raven", game));
        Track dawn = tracks.save(new Track("dawn", game));

        rename("/api/admin/tracks/" + dawn.getId(), "Dawn")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Dawn"))
            .andExpect(jsonPath("$.gameName").value("Stellar Blade"));

        rename("/api/admin/tracks/" + dawn.getId(), "raven")
            .andExpect(status().isConflict());
    }

    private ResultActions rename(String url, String name) throws Exception {
        return mockMvc.perform(patch(url)
            .header("Authorization", "Bearer " + tokenStore.create("admin"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new RenameRequest(name))));
    }
}
