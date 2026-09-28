package fr.insalan.blindtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.StringReader;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import fr.insalan.blindtest.model.Franchise;
import fr.insalan.blindtest.model.Game;
import fr.insalan.blindtest.model.Knowledge;
import fr.insalan.blindtest.model.Listener;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.repository.FranchiseRepository;
import fr.insalan.blindtest.repository.GameRepository;
import fr.insalan.blindtest.repository.KnowledgeRepository;
import fr.insalan.blindtest.repository.ListenerRepository;
import fr.insalan.blindtest.repository.TrackRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExportControllerTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    FranchiseRepository franchises;

    @Autowired
    GameRepository games;

    @Autowired
    TrackRepository tracks;

    @Autowired
    ListenerRepository listeners;

    @Autowired
    KnowledgeRepository knowledge;

    @AfterEach
    void cleanDatabase() {
        knowledge.deleteAll();
        tracks.deleteAll();
        games.deleteAll();
        franchises.deleteAll();
        listeners.deleteAll();
    }

    private List<CSVRecord> exportRecords() throws Exception {
        String body = mockMvc.perform(get("/api/export/sheet"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        try (CSVParser parser = CSVFormat.DEFAULT.parse(new StringReader(body))) {
            return parser.getRecords();
        }
    }

    @Test
    void exportsATrackWithItsVotesAndLinks() throws Exception {
        Franchise franchise = franchises.save(new Franchise("Stellar Blade"));
        Game game = games.save(new Game("Stellar Blade", franchise));
        Track dawn = new Track("Dawn", game);
        dawn.setKhinsiderLink("https://downloads.khinsider.com/dawn");
        dawn.setYoutubeLink("https://www.youtube.com/watch?v=dawn");
        tracks.save(dawn);
        Listener alice = listeners.save(new Listener("Alice"));
        Listener bob = listeners.save(new Listener("Bob"));
        knowledge.save(new Knowledge(alice, dawn, true));
        knowledge.save(new Knowledge(bob, dawn, false));

        List<CSVRecord> records = exportRecords();

        CSVRecord header = records.get(0);
        int aliceColumn = header.toList().indexOf("Alice");
        int bobColumn = header.toList().indexOf("Bob");
        assertThat(aliceColumn).isPositive();
        assertThat(bobColumn).isPositive();

        CSVRecord row = records.get(1);
        assertThat(row.get(0)).isEqualTo("Stellar Blade");
        assertThat(row.get(1)).isEqualTo("Stellar Blade");
        assertThat(row.get(2)).isEqualTo("Dawn");
        assertThat(row.get(4)).isEqualTo("https://downloads.khinsider.com/dawn");
        assertThat(row.get(6)).isEqualTo("https://www.youtube.com/watch?v=dawn");
        assertThat(row.get(7)).isEqualTo("2");
        assertThat(row.get(8)).isEqualTo("0.50");
        assertThat(row.get(aliceColumn)).isEqualTo("1");
        assertThat(row.get(bobColumn)).isEqualTo("0");
    }

    @Test
    void exportsAGameWithoutAnyTrack() throws Exception {
        Franchise franchise = franchises.save(new Franchise("Stellar Blade"));
        games.save(new Game("Jeu sans musique", franchise));

        List<CSVRecord> records = exportRecords();

        CSVRecord row = records.get(1);
        assertThat(row.get(0)).isEqualTo("Stellar Blade");
        assertThat(row.get(1)).isEqualTo("Jeu sans musique");
        assertThat(row.get(2)).isEmpty();
        assertThat(row.get(7)).isEqualTo("0");
        assertThat(row.get(8)).isEmpty();
    }

    @Test
    void exportsAFranchiseWithoutAnyGame() throws Exception {
        franchises.save(new Franchise("Franchise sans jeu"));

        List<CSVRecord> records = exportRecords();

        CSVRecord row = records.get(1);
        assertThat(row.get(0)).isEqualTo("Franchise sans jeu");
        assertThat(row.get(1)).isEmpty();
        assertThat(row.get(2)).isEmpty();
    }
}
