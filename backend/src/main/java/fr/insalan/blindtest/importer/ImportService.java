package fr.insalan.blindtest.importer;

import java.util.List;

import org.springframework.stereotype.Service;

import fr.insalan.blindtest.model.Franchise;
import fr.insalan.blindtest.model.Game;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.repository.FranchiseRepository;
import fr.insalan.blindtest.repository.GameRepository;
import fr.insalan.blindtest.repository.TrackRepository;
import jakarta.transaction.Transactional;


/*
 * Enregistre en base les lignes lues dans le Google Sheet selon les règles établies dans docs/import-gsheet.md.
 */
@Service 
public class ImportService {
    
    private final FranchiseRepository franchiseRepository;
    private final GameRepository gameRepository;
    private final TrackRepository trackRepository;

    public ImportService(FranchiseRepository franchiseRepository, GameRepository gameRepository, TrackRepository trackRepository) {
        this.franchiseRepository = franchiseRepository;
        this.gameRepository = gameRepository;
        this.trackRepository = trackRepository;
    }

    // Une seule transaction, si une ligne pose problème, rien n'est enregistré en base.
    @Transactional
    public ImportReport importRows(List<SheetRow> rows) {
        long franchisesBefore = franchiseRepository.count();
        long gamesBefore = gameRepository.count();
        long tracksBefore = trackRepository.count();
        int rowsIgnored = 0;

        for (SheetRow row : rows) {
            // Un jeu sans licence ou une musique sans jeu ne peut être rattaché à rien
            if (row.franchise() == null || (row.game() == null && row.track() != null)) {
                rowsIgnored++;
                continue;
            }

            Franchise franchise = findOrCreateFranchise(row.franchise());
            if (row.game() == null) {
                continue;
            }

            Game game = findOrCreateGame(franchise, row.game());
            if (row.track() == null) {
                continue;
            }

            saveTrack(game, row);
        }

        return new ImportReport(
            (int)(franchiseRepository.count() - franchisesBefore),
            (int)(gameRepository.count() - gamesBefore),
            (int)(trackRepository.count() - tracksBefore),
            rowsIgnored
        );
    }

    private Franchise findOrCreateFranchise(String name) {
        return franchiseRepository.findByName(name)
            .orElseGet(() -> franchiseRepository.save(new Franchise(name)));
    }

    private Game findOrCreateGame(Franchise franchise, String name) {
        return gameRepository.findByFranchiseAndName(franchise, name)
            .orElseGet(() -> gameRepository.save(new Game(name, franchise)));
    }

        private void saveTrack(Game game, SheetRow row) {
        Track track = trackRepository.findByGameAndName(game, row.track())
                .orElseGet(() -> new Track(row.track(), game));
        
        if (row.khinsiderLink() != null) {
            track.setKhinsiderLink(row.khinsiderLink());
        }
        if (row.youtubeLink() != null) {
            track.setYoutubeLink(row.youtubeLink());
        }
        trackRepository.save(track);
    }
}
