package fr.insalan.blindtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import fr.insalan.blindtest.importer.ImportRunner;
import fr.insalan.blindtest.repository.ListenerRepository;
import fr.insalan.blindtest.repository.TrackRepository;


@SpringBootTest 
@ActiveProfiles ("test")
@Transactional
public class ImportRunnerTests {

    @Autowired
    ImportRunner importRunner;

    @Autowired
    TrackRepository trackRepository;

    @Autowired
    ListenerRepository listenerRepository;

    @Test
    void importsTheFileGivenInTheImportOption() throws Exception {
        Path file = Path.of(getClass().getResource("/import/stellar-blade.csv").toURI());

        importRunner.run(new DefaultApplicationArguments("--import=" + file));

        assertEquals(14, trackRepository.count());
        assertEquals(8, listenerRepository.count());
    }

    @Test
    void doesNothingWithoutTheImportOption() throws Exception {
        importRunner.run(new DefaultApplicationArguments());

        assertEquals(0, trackRepository.count());
    }

    @Test
    void failsClearlyWhenTheFileDoesNotExist() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
            () -> importRunner.run(new DefaultApplicationArguments("--import=n-existe-pas.csv")));

        assertTrue(error.getMessage().contains("n-existe-pas.csv"));
    }

    @Test
    void failsWhenNoFileIsGiven() {
        assertThrows(IllegalArgumentException.class,
            () -> importRunner.run(new DefaultApplicationArguments("--import")));
    }

    
}
