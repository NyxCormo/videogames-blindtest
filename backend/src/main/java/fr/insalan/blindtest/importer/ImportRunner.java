package fr.insalan.blindtest.importer;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;


/*
 * Lance l'import d'un fichier CSV depuis la ligne de commande : --import=chemin/vers/fichier.csv
 * Sans cette option, l'application démarre normalement.
 */
@Component 
public class ImportRunner implements ApplicationRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(ImportRunner.class);

    private final ImportService importService;

    public ImportRunner(ImportService importService) {
        this.importService = importService;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        if (!args.containsOption("import")) {
            return;
        }

        List<String> values = args.getOptionValues("import");
        if (values == null || values.isEmpty() || values.get(0).isBlank()) {
            throw new IllegalArgumentException("L'option --import doit être suivie du chemin vers le fichier CSV à importer.");
        }

        Path file = Path.of(values.get(0));
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("Fichier introuvable : " + file);
        }

        List<SheetRow> rows;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            rows = new SheetReader().read(reader);
        }
        ImportReport report = importService.importRows(rows);

        logger.info("Import terminé : {} franchises, {} jeux, {} musiques, {} votants et {} votes enregistrés, {} lignes ignorées.",
            report.franchisesCreated(),
            report.gamesCreated(),
            report.tracksCreated(),
            report.listenersCreated(),
            report.votesCreated(),
            report.rowsIgnored()
        );
    }
}
