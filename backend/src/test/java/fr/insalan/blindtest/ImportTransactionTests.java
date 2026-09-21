package fr.insalan.blindtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import fr.insalan.blindtest.importer.ImportService;
import fr.insalan.blindtest.importer.SheetRow;
import fr.insalan.blindtest.repository.FranchiseRepository;
import fr.insalan.blindtest.repository.GameRepository;
import fr.insalan.blindtest.repository.KnowledgeRepository;
import fr.insalan.blindtest.repository.ListenerRepository;
import fr.insalan.blindtest.repository.TrackRepository;


@SpringBootTest
@ActiveProfiles("test")
public class ImportTransactionTests {

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

    @AfterEach
    void cleanDatabase() {
        knowledgeRepository.deleteAll();
        trackRepository.deleteAll();
        gameRepository.deleteAll();
        franchiseRepository.deleteAll();
        listenerRepository.deleteAll();
    }

    @Test
    void nothingIsSavedWhenTheImportFails() {
        // Un votant sans nom fait échouer l'import, après l'enregistrement de la musique précédente
        Map<String, Boolean> invalidVotes = new HashMap<>();
        invalidVotes.put(null, true);
        List<SheetRow> rows = List.of(
            new SheetRow("Stellar Blade (Licence)", "Stellar Blade", "Dawn", null, null, Map.of()),
            new SheetRow("Stellar Blade (Licence)", "Stellar Blade", "Raven", null, null, invalidVotes)
        );

        assertThrows(RuntimeException.class, () -> importService.importRows(rows));

        assertEquals(0, franchiseRepository.count());
        assertEquals(0, gameRepository.count());
        assertEquals(0, trackRepository.count());
    }
}
