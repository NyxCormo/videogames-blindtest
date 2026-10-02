package fr.insalan.blindtest.deletion;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import fr.insalan.blindtest.model.Blindtest;
import fr.insalan.blindtest.model.Franchise;
import fr.insalan.blindtest.model.Game;
import fr.insalan.blindtest.model.Listener;
import fr.insalan.blindtest.model.Tag;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.repository.BlindtestDifficultyBandRepository;
import fr.insalan.blindtest.repository.BlindtestRepository;
import fr.insalan.blindtest.repository.BlindtestScoreRepository;
import fr.insalan.blindtest.repository.BlindtestTrackRepository;
import fr.insalan.blindtest.repository.FranchiseRepository;
import fr.insalan.blindtest.repository.GameRepository;
import fr.insalan.blindtest.repository.KnowledgeRepository;
import fr.insalan.blindtest.repository.ListenerRepository;
import fr.insalan.blindtest.repository.TagRepository;
import fr.insalan.blindtest.repository.TrackRepository;
import fr.insalan.blindtest.repository.TrackTagRepository;
import jakarta.transaction.Transactional;

// La base n'a pas de suppression en cascade : on supprime d'abord ce qui pointe vers l'élément, puis l'élément.
// Chaque suppression repasse par son aperçu, pour que la page et le serveur appliquent la même règle.
@Service
public class DeletionService {

    private final FranchiseRepository franchiseRepository;
    private final GameRepository gameRepository;
    private final TrackRepository trackRepository;
    private final TrackTagRepository trackTagRepository;
    private final KnowledgeRepository knowledgeRepository;
    private final BlindtestRepository blindtestRepository;
    private final BlindtestTrackRepository blindtestTrackRepository;
    private final BlindtestScoreRepository blindtestScoreRepository;
    private final BlindtestDifficultyBandRepository blindtestDifficultyBandRepository;
    private final ListenerRepository listenerRepository;
    private final TagRepository tagRepository;

    public DeletionService(
        FranchiseRepository franchiseRepository,
        GameRepository gameRepository,
        TrackRepository trackRepository,
        TrackTagRepository trackTagRepository,
        KnowledgeRepository knowledgeRepository,
        BlindtestRepository blindtestRepository,
        BlindtestTrackRepository blindtestTrackRepository,
        BlindtestScoreRepository blindtestScoreRepository,
        BlindtestDifficultyBandRepository blindtestDifficultyBandRepository,
        ListenerRepository listenerRepository,
        TagRepository tagRepository
    ) {
        this.franchiseRepository = franchiseRepository;
        this.gameRepository = gameRepository;
        this.trackRepository = trackRepository;
        this.trackTagRepository = trackTagRepository;
        this.knowledgeRepository = knowledgeRepository;
        this.blindtestRepository = blindtestRepository;
        this.blindtestTrackRepository = blindtestTrackRepository;
        this.blindtestScoreRepository = blindtestScoreRepository;
        this.blindtestDifficultyBandRepository = blindtestDifficultyBandRepository;
        this.listenerRepository = listenerRepository;
        this.tagRepository = tagRepository;
    }

    // Une franchise ou un jeu qui contient encore quelque chose se fusionne ou se vide d'abord : pas de suppression en chaîne.
    public DeletePreview previewFranchise(Integer id) {
        Franchise franchise = franchiseRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Franchise introuvable"));
        long games = gameRepository.countByFranchiseId(id);
        if (games > 0) {
            long tracks = trackRepository.countByFranchiseId(id);
            return new DeletePreview(false, "« " + franchise.getName() + " » contient " + plural(games, "jeu", "jeux")
                + " et " + plural(tracks, "musique", "musiques") + " : la fusionner ou la vider d'abord.");
        }
        return new DeletePreview(true, "« " + franchise.getName() + " » ne contient aucun jeu.");
    }

    @Transactional
    public void deleteFranchise(Integer id) {
        requireAllowed(previewFranchise(id));
        franchiseRepository.deleteById(id);
    }

    public DeletePreview previewGame(Integer id) {
        Game game = gameRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jeu introuvable"));
        long tracks = trackRepository.countByGameId(id);
        if (tracks > 0) {
            return new DeletePreview(false, "« " + game.getName() + " » contient " + plural(tracks, "musique", "musiques")
                + " : le fusionner ou le vider d'abord.");
        }
        return new DeletePreview(true, "« " + game.getName() + " » ne contient aucune musique.");
    }

    @Transactional
    public void deleteGame(Integer id) {
        requireAllowed(previewGame(id));
        gameRepository.deleteById(id);
    }

    // Une musique déjà votée ou jouée se fusionne : la supprimer ferait perdre ces données.
    public DeletePreview previewTrack(Integer id) {
        Track track = trackRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Musique introuvable"));
        long votes = knowledgeRepository.countByTrackId(id);
        long blindtests = blindtestTrackRepository.countBlindtestsWithTrack(id);
        if (votes > 0 || blindtests > 0) {
            return new DeletePreview(false, "« " + track.getName() + " » a " + plural(votes, "vote", "votes")
                + " et apparaît dans " + plural(blindtests, "blindtest", "blindtests") + " : la fusionner plutôt que la supprimer.");
        }
        long tags = trackTagRepository.countByTrackId(id);
        String tagText = tags == 0 ? "" : tags == 1 ? " Son tag lui sera retiré." : " Ses " + tags + " tags lui seront retirés.";
        return new DeletePreview(true, "« " + track.getName() + " » n'a ni vote ni blindtest." + tagText);
    }

    @Transactional
    public void deleteTrack(Integer id) {
        requireAllowed(previewTrack(id));
        trackTagRepository.deleteByTrackId(id);
        trackRepository.deleteById(id);
    }

    public DeletePreview previewBlindtest(Integer id) {
        Blindtest blindtest = blindtestRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blindtest introuvable"));
        long tracks = blindtestTrackRepository.countByBlindtestId(id);
        long scores = blindtestScoreRepository.countByBlindtestId(id);
        return new DeletePreview(true, "Le blindtest « " + blindtest.getName() + " » sera supprimé avec sa liste de "
            + plural(tracks, "musique", "musiques") + " (les musiques elles-mêmes restent) et "
            + plural(scores, "score", "scores") + ".");
    }

    @Transactional
    public void deleteBlindtest(Integer id) {
        requireAllowed(previewBlindtest(id));
        blindtestTrackRepository.deleteByBlindtestId(id);
        blindtestScoreRepository.deleteByBlindtestId(id);
        blindtestDifficultyBandRepository.deleteByBlindtestId(id);
        blindtestRepository.deleteById(id);
    }

    public DeletePreview previewListener(Integer id) {
        Listener listener = listenerRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pseudo introuvable"));
        long votes = knowledgeRepository.countByListenerId(id);
        long scores = blindtestScoreRepository.countByListenerId(id);
        return new DeletePreview(true, "Le pseudo « " + listener.getName() + " » sera supprimé avec "
            + plural(votes, "vote", "votes") + " et " + plural(scores, "score", "scores") + " de blindtest.");
    }

    @Transactional
    public void deleteListener(Integer id) {
        requireAllowed(previewListener(id));
        knowledgeRepository.deleteByListenerId(id);
        blindtestScoreRepository.deleteByListenerId(id);
        listenerRepository.deleteById(id);
    }

    public DeletePreview previewTag(Integer id) {
        Tag tag = tagRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag introuvable"));
        long tracks = trackTagRepository.countByTagId(id);
        return new DeletePreview(true, "Le tag « " + tag.getName() + " » sera retiré de "
            + plural(tracks, "musique", "musiques") + ".");
    }

    // Retire le tag de toutes les musiques qui l'avaient, puis le supprime.
    @Transactional
    public void deleteTag(Integer id) {
        requireAllowed(previewTag(id));
        trackTagRepository.deleteByTagId(id);
        tagRepository.deleteById(id);
    }

    private void requireAllowed(DeletePreview preview) {
        if (!preview.allowed()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, preview.impact());
        }
    }

    private static String plural(long count, String singular, String pluralForm) {
        return count + " " + (count > 1 ? pluralForm : singular);
    }
}
