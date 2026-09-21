package fr.insalan.blindtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import fr.insalan.blindtest.model.Knowledge;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.repository.FranchiseRepository;
import fr.insalan.blindtest.repository.GameRepository;
import fr.insalan.blindtest.repository.KnowledgeRepository;
import fr.insalan.blindtest.repository.ListenerRepository;
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

    @Autowired
    ListenerRepository listenerRepository;

    @Autowired
    KnowledgeRepository knowledgeRepository;


    private static SheetRow row(String franchise, String game, String track) {
        return new SheetRow(franchise, game, track, null, null, Map.of());
    }

    private static SheetRow row(String franchise, String game, String track, String khinsiderLink) {
        return new SheetRow(franchise, game, track, khinsiderLink, null, Map.of());
    }

    private static SheetRow row(String franchise, String game, String track, Map<String, Boolean> votes) {
        return new SheetRow(franchise, game, track, null, null, votes);
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

        // Une franchise, deux jeux, quatorze musiques, huit votants et leurs 55 votes
        assertEquals(new ImportReport(1, 2, 14, 8, 55, 0), report);
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

        assertEquals(new ImportReport(0, 0, 0, 0, 0, 0), report);
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

        assertEquals(new ImportReport(2, 1, 0, 0, 0, 0), report);
    }

    @Test
    void sameGameUnderTwoFranchisesGivesTwoGames() {
        ImportReport report = importService.importRows(List.of(
            row("Stellar Blade (Licence)", "Stellar Blade", "Dawn"),
            row("Stellar Blade (Fanmade)", "Stellar Blade", "Dawn")
        ));

        assertEquals(new ImportReport(2, 2, 2, 0, 0, 0), report);
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

        importService.importRows(List.of(row("Stellar Blade", "Stellar Blade", "Dawn")));
        assertEquals(link, trackRepository.findAll().get(0).getKhinsiderLink());
    }

    @Test
    void ignoresRowsThatCannotBeAttached() {
        ImportReport report = importService.importRows(List.of(
            row(null, "Stellar Blade", "Dawn"),
            row("Stellar Blade (Licence)", null, "Dawn")
        ));

        assertEquals(new ImportReport(0, 0, 0, 0, 0, 2), report);
    }

    @Test
    void importsVotersAndTheirVotes() {
        ImportReport report = importService.importRows(List.of(
            row("Stellar Blade (Licence)", "Stellar Blade", "Dawn", Map.of("UserA", true, "UserB", false)),
            row("Stellar Blade (Licence)", "Stellar Blade", "Raven", Map.of("UserA", false))
        ));

        // Deux votants (UserA a voté deux fois) et trois votes
        assertEquals(new ImportReport(1, 1, 2, 2, 3, 0), report);
        assertEquals(2, listenerRepository.count());
        assertEquals(1, knowledgeRepository.findAll().stream().filter(Knowledge::isKnows).count());
    }

    @Test
    void reimportUpdatesAVoteWithoutDuplicatingIt() {
        importService.importRows(List.of(
            row("Stellar Blade (Licence)", "Stellar Blade", "Dawn", Map.of("UserA", true))));

        ImportReport report = importService.importRows(List.of(
            row("Stellar Blade (Licence)", "Stellar Blade", "Dawn", Map.of("UserA", false))));

        assertEquals(new ImportReport(0, 0, 0, 0, 0, 0), report);
        assertEquals(1, knowledgeRepository.count());
        assertFalse(knowledgeRepository.findAll().get(0).isKnows());
    }

    @Test
    void reimportWithoutVoteKeepsTheExistingVote() {
        importService.importRows(List.of(
            row("Stellar Blade (Licence)", "Stellar Blade", "Dawn", Map.of("UserA", true))));

        importService.importRows(List.of(row("Stellar Blade (Licence)", "Stellar Blade", "Dawn")));

        assertEquals(1, knowledgeRepository.count());
        assertTrue(knowledgeRepository.findAll().get(0).isKnows());
    }

    @Test
    void ignoresVotesOfRowsWithoutTrack() {
        ImportReport report = importService.importRows(List.of(
            row("Stellar Blade (Licence)", "Stellar Blade", null, Map.of("UserA", true))));

        assertEquals(new ImportReport(1, 1, 0, 0, 0, 0), report);
    }

}
