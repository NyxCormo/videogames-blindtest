package fr.insalan.blindtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.StringReader;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import fr.insalan.blindtest.importer.SheetReader;
import fr.insalan.blindtest.importer.SheetRow;
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

class ExportControllerTests extends IntegrationTest {

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

    private String exportBody() throws Exception {
        return mockMvc.perform(get("/api/export/sheet"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    }

    private List<CSVRecord> exportRecords() throws Exception {
        String body = exportBody();
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

        CSVRecord header = records.get(3);
        int aliceColumn = header.toList().indexOf("Alice");
        int bobColumn = header.toList().indexOf("Bob");
        assertThat(aliceColumn).isPositive();
        assertThat(bobColumn).isPositive();

        CSVRecord row = records.get(4);
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
    void exportCanBeReadBackByTheImport() throws Exception {
        Franchise franchise = franchises.save(new Franchise("Stellar Blade"));
        Game game = games.save(new Game("Stellar Blade", franchise));
        Track dawn = tracks.save(new Track("Dawn", game));
        Track raven = tracks.save(new Track("Raven", game));
        Listener alice = listeners.save(new Listener("Alice"));
        Listener bob = listeners.save(new Listener("Bob"));
        knowledge.save(new Knowledge(alice, dawn, true));
        knowledge.save(new Knowledge(bob, dawn, false));
        knowledge.save(new Knowledge(bob, raven, true));

        List<SheetRow> rows = new SheetReader().read(new StringReader(exportBody()));

        assertThat(rows).extracting(SheetRow::track).containsExactly("Dawn", "Raven");
        assertThat(rows.get(0).votes()).containsOnly(entry("Alice", true), entry("Bob", false));
        assertThat(rows.get(1).votes()).containsOnly(entry("Bob", true));
    }

    @Test
    void exportsAGameWithoutAnyTrack() throws Exception {
        Franchise franchise = franchises.save(new Franchise("Stellar Blade"));
        games.save(new Game("Jeu sans musique", franchise));

        List<CSVRecord> records = exportRecords();

        CSVRecord row = records.get(4);
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

        CSVRecord row = records.get(4);
        assertThat(row.get(0)).isEqualTo("Franchise sans jeu");
        assertThat(row.get(1)).isEmpty();
        assertThat(row.get(2)).isEmpty();
    }
}
