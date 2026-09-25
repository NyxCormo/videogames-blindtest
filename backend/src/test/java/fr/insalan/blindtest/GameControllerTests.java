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
class GameControllerTests {

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
    void listReturnsEveryGameSortedByNameWithItsFranchise() throws Exception {
        Franchise stellarBladeFranchise = franchises.save(new Franchise("Stellar Blade"));
        games.save(new Game("Stellar Blade", stellarBladeFranchise));
        Franchise finalFantasy = franchises.save(new Franchise("Final Fantasy"));
        games.save(new Game("Final Fantasy VII", finalFantasy));

        mockMvc.perform(get("/api/games"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Final Fantasy VII"))
            .andExpect(jsonPath("$[0].franchiseName").value("Final Fantasy"))
            .andExpect(jsonPath("$[1].name").value("Stellar Blade"))
            .andExpect(jsonPath("$[1].franchiseName").value("Stellar Blade"));
    }

    @Test
    void tracksReturnsOnlyTheTracksOfThatGame() throws Exception {
        Franchise franchise = franchises.save(new Franchise("Stellar Blade"));
        Game stellarBlade = games.save(new Game("Stellar Blade", franchise));
        Game otherGame = games.save(new Game("Stellar Blade: Blood Rain", franchise));
        tracks.save(new Track("Dawn", stellarBlade));
        tracks.save(new Track("Raven", stellarBlade));
        tracks.save(new Track("Other game track", otherGame));

        mockMvc.perform(get("/api/games/" + stellarBlade.getId() + "/tracks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].name").value("Dawn"))
            .andExpect(jsonPath("$[1].name").value("Raven"));
    }

    @Test
    void applyTagToGameTagsEveryTrackOfTheGame() throws Exception {
        Franchise franchise = franchises.save(new Franchise("Stellar Blade"));
        Game game = games.save(new Game("Stellar Blade", franchise));
        Track dawn = tracks.save(new Track("Dawn", game));
        Track raven = tracks.save(new Track("Raven", game));
        TagType genre = tagTypes.findByName("genre").orElseThrow();
        Tag rpg = tags.save(new Tag("RPG", genre));

        mockMvc.perform(post("/api/games/" + game.getId() + "/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddTagRequest(rpg.getId()))))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tracks/" + dawn.getId() + "/tags"))
            .andExpect(jsonPath("$[0].name").value("RPG"));
        mockMvc.perform(get("/api/tracks/" + raven.getId() + "/tags"))
            .andExpect(jsonPath("$[0].name").value("RPG"));
    }

    @Test
    void applyTagToGameIsIdempotentForTracksThatAlreadyHaveIt() throws Exception {
        Franchise franchise = franchises.save(new Franchise("Stellar Blade"));
        Game game = games.save(new Game("Stellar Blade", franchise));
        Track dawn = tracks.save(new Track("Dawn", game));
        TagType genre = tagTypes.findByName("genre").orElseThrow();
        Tag rpg = tags.save(new Tag("RPG", genre));
        AddTagRequest request = new AddTagRequest(rpg.getId());

        mockMvc.perform(post("/api/games/" + game.getId() + "/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/games/" + game.getId() + "/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tracks/" + dawn.getId() + "/tags"))
            .andExpect(jsonPath("$.length()").value(1));
    }
}
