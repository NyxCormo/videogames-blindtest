package fr.insalan.blindtest.importer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

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
import jakarta.transaction.Transactional;


/*
 * Enregistre en base les lignes lues dans le Google Sheet selon les règles établies dans docs/import-gsheet.md.
 */
@Service 
public class ImportService {
    
    private final FranchiseRepository franchiseRepository;
    private final GameRepository gameRepository;
    private final TrackRepository trackRepository;
    private final ListenerRepository listenerRepository;
    private final KnowledgeRepository knowledgeRepository;

    public ImportService(
        FranchiseRepository franchiseRepository, 
        GameRepository gameRepository, 
        TrackRepository trackRepository,
        ListenerRepository listenerRepository,
        KnowledgeRepository knowledgeRepository
    ) {
        this.franchiseRepository = franchiseRepository;
        this.gameRepository = gameRepository;
        this.trackRepository = trackRepository;
        this.listenerRepository = listenerRepository;
        this.knowledgeRepository = knowledgeRepository;
    }

    // Une seule transaction, si une ligne pose problème, rien n'est enregistré en base.
    @Transactional
    public ImportReport importRows(List<SheetRow> rows) {
        long franchisesBefore = franchiseRepository.count();
        long gamesBefore = gameRepository.count();
        long tracksBefore = trackRepository.count();
        long listenersBefore = listenerRepository.count();
        long votesBefore = knowledgeRepository.count();
        int rowsIgnored = 0;
        // Ont garde les votants en mémoire pour ne pas les recréer à chaque ligne
        Map<String, Listener> listeners = new HashMap<>();

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

            Track track = saveTrack(game, row);
            saveVotes(track, row.votes(), listeners);
        }

        return new ImportReport(
            (int)(franchiseRepository.count() - franchisesBefore),
            (int)(gameRepository.count() - gamesBefore),
            (int)(trackRepository.count() - tracksBefore),
            (int)(listenerRepository.count() - listenersBefore),
            (int)(knowledgeRepository.count() - votesBefore),
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

        private Track saveTrack(Game game, SheetRow row) {
        Track track = trackRepository.findByGameAndName(game, row.track())
                .orElseGet(() -> new Track(row.track(), game));
        
        if (row.khinsiderLink() != null) {
            track.setKhinsiderLink(row.khinsiderLink());
        }
        if (row.youtubeLink() != null) {
            track.setYoutubeLink(row.youtubeLink());
        }
        return trackRepository.save(track);
    }

    private Listener findOrCreateListener(String name){
        return listenerRepository.findByName(name)
            .orElseGet(() -> listenerRepository.save(new Listener(name)));
    }

    private void saveVotes(Track track, Map<String, Boolean> votes, Map<String, Listener> listeners) {
        votes.forEach((voterName, knows) -> {
            Listener listener = listeners.computeIfAbsent(voterName, this::findOrCreateListener);
            // save met à jour le vote s'il existe déjà, sinon il le crée
            knowledgeRepository.save(new Knowledge(listener, track, knows));
        });
    }
}
