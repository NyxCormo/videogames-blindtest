package fr.insalan.blindtest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import fr.insalan.blindtest.importer.ImportReport;
import fr.insalan.blindtest.importer.ImportService;
import fr.insalan.blindtest.importer.SheetReader;
import fr.insalan.blindtest.importer.SheetRow;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.repository.FranchiseRepository;
import fr.insalan.blindtest.repository.GameRepository;
import fr.insalan.blindtest.repository.TrackRepository;

@SpringBootTest 
@ActiveProfiles("test")
@Transactional
public class ImportServiceTests {
    
    @Autowired 
    ImportService importService;

    @Autowired 
    FranchiseRepository franchiseRepository;

    @Autowired
    GameRepository gameRepository;

    @Autowired
    TrackRepository trackRepository;

    private static SheetRow row(String franchise, String game, String track) {
        return new SheetRow(franchise, game, track, null, null, Map.of());
    }

    private static SheetRow row(String franchise, String game, String track, String khinsiderLink) {
        return new SheetRow(franchise, game, track, khinsiderLink, null, Map.of());
    }

    @Test 
    void importStellarBladeFile() throws IOException {
        List<SheetRow> rows;
        try (Reader file = new InputStreamReader(
            getClass().getResourceAsStream("/import/stellar-blade.csv"), StandardCharsets.UTF_8
        )){
            rows = new SheetReader().read(file);
        }

        ImportReport report = importService.importRows(rows);

        // Une franchise, deux jeux, quatorze musiques
        assertEquals(new ImportReport(1, 2, 14, 0), report);
        assertEquals(14, trackRepository.count());
        Track dawn = trackRepository.findAll().stream().filter(track -> track.getName().equals("Dawn")).findFirst().orElseThrow();
        assertEquals(rows.get(0).khinsiderLink(), dawn.getKhinsiderLink());
    }

    @Test 
    void importingTwiceCreatesNothingNew() {
        List<SheetRow> rows = List.of(
            row("Stellar Blade (Licence)", "Stellar Blade", "Dawn"),
            row("Stellar Blade (Licence)", "Stellar Blade", "Raven")
        );

        importService.importRows(rows);
        ImportReport report = importService.importRows(rows);

        assertEquals(new ImportReport(0, 0, 0, 0), report);
        assertEquals(1, franchiseRepository.count());
        assertEquals(1, gameRepository.count());
        assertEquals(2, trackRepository.count());
    }

    @Test 
    void keepsFranchiseAndGameWithoutTrack() {
        ImportReport report = importService.importRows(List.of(
            row("Stellar Blade (Licence)", "Stellar Blade", null),
            row("Stellar Blade", null, null)
        ));

        assertEquals(new ImportReport(2, 1, 0, 0), report);
    }

    @Test
    void sameGameUnderTwoFranchisesGivesTwoGames() {
        ImportReport report = importService.importRows(List.of(
            row("Stellar Blade (Licence)", "Stellar Blade", "Dawn"),
            row("Stellar Blade (Fanmade)", "Stellar Blade", "Dawn")
        ));

        assertEquals(new ImportReport(2, 2, 2, 0), report);
    }

    @Test
    void namesDifferingOnlyByCaseStaySeparate() {
        ImportReport report = importService.importRows(List.of(
                row("Stellar Blade", "Stellar Blade", "Dawn"),
                row("Stellar blade", "Stellar Blade", "Dawn")));

        assertEquals(2, report.franchisesCreated());
    }

    @Test
    void updatesLinksButNeverErasesThem() {
        String link = "https://downloads.khinsider.com/game-soundtracks/album/stellar-blade";
        importService.importRows(List.of(row("Stellar Blade", "Stellar Blade", "Dawn")));

        importService.importRows(List.of(row("Stellar Blade", "Stellar Blade", "Dawn", link)));
        assertEquals(link, trackRepository.findAll().get(0).getKhinsiderLink());
    }

    @Test
    void ignoresRowsThatCannotBeAttached() {
        ImportReport report = importService.importRows(List.of(
            row(null, "Stellar Blade", "Dawn"),
            row("Stellar Blade (Licence)", null, "Dawn")
        ));

        assertEquals(new ImportReport(0, 0, 0, 2), report);
    }

}
