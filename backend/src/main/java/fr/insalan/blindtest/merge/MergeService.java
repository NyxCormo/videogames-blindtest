package fr.insalan.blindtest.merge;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import fr.insalan.blindtest.model.Franchise;
import fr.insalan.blindtest.model.Game;
import fr.insalan.blindtest.model.Listener;
import fr.insalan.blindtest.model.Tag;
import fr.insalan.blindtest.model.Track;
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

// Chaque fusion déplace tout vers l'élément gardé puis supprime l'autre, dans une seule transaction :
// si une étape échoue, rien n'est modifié. Elle repasse par son aperçu, comme les suppressions.
@Service
public class MergeService {

    private final FranchiseRepository franchiseRepository;
    private final GameRepository gameRepository;
    private final TrackRepository trackRepository;
    private final KnowledgeRepository knowledgeRepository;
    private final TrackTagRepository trackTagRepository;
    private final BlindtestTrackRepository blindtestTrackRepository;
    private final ListenerRepository listenerRepository;
    private final BlindtestScoreRepository blindtestScoreRepository;
    private final TagRepository tagRepository;

    public MergeService(
        FranchiseRepository franchiseRepository,
        GameRepository gameRepository,
        TrackRepository trackRepository,
        KnowledgeRepository knowledgeRepository,
        TrackTagRepository trackTagRepository,
        BlindtestTrackRepository blindtestTrackRepository,
        ListenerRepository listenerRepository,
        BlindtestScoreRepository blindtestScoreRepository,
        TagRepository tagRepository
    ) {
        this.franchiseRepository = franchiseRepository;
        this.gameRepository = gameRepository;
        this.trackRepository = trackRepository;
        this.knowledgeRepository = knowledgeRepository;
        this.trackTagRepository = trackTagRepository;
        this.blindtestTrackRepository = blindtestTrackRepository;
        this.listenerRepository = listenerRepository;
        this.blindtestScoreRepository = blindtestScoreRepository;
        this.tagRepository = tagRepository;
    }

    public MergePreview previewTracks(Integer sourceId, Integer targetId) {
        requireDifferent(sourceId, targetId);
        Track source = trackRepository.findByIdWithGameAndFranchise(sourceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Musique introuvable"));
        Track target = trackRepository.findByIdWithGameAndFranchise(targetId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Musique cible inconnue"));
        // Le jeu entre parenthèses distingue deux musiques homonymes, le cas le plus courant de fusion.
        StringBuilder summary = new StringBuilder("« " + source.getName() + " » (" + source.getGame().getName()
            + ") sera supprimée. « " + target.getName() + " » (" + target.getGame().getName() + ") récupère " + plural(knowledgeRepository.countByTrackId(sourceId), "vote", "votes")
            + ", " + plural(trackTagRepository.countByTrackId(sourceId), "tag", "tags")
            + " et " + plural(blindtestTrackRepository.countBlindtestsWithTrack(sourceId), "blindtest", "blindtests"));
        if (target.getKhinsiderLink() == null && source.getKhinsiderLink() != null) {
            summary.append(", ainsi que son lien KHInsider");
        }
        if (target.getYoutubeLink() == null && source.getYoutubeLink() != null) {
            summary.append(", ainsi que son lien YouTube");
        }
        summary.append(".");
        long both = knowledgeRepository.countListenersWhoVotedForBoth(sourceId, targetId);
        if (both > 0) {
            summary.append(both == 1
                ? " 1 personne a voté pour les deux : son vote pour « " + target.getName() + " » est gardé."
                : " " + both + " personnes ont voté pour les deux : leur vote pour « " + target.getName() + " » est gardé.");
        }
        return new MergePreview(true, summary.toString());
    }

    @Transactional
    public void mergeTracks(Integer sourceId, Integer targetId) {
        previewTracks(sourceId, targetId);
        Track source = trackRepository.findById(sourceId).orElseThrow();
        Track target = trackRepository.findById(targetId).orElseThrow();

        if (target.getKhinsiderLink() == null && source.getKhinsiderLink() != null) {
            target.setKhinsiderLink(source.getKhinsiderLink());
            target.setAudioLink(source.getAudioLink());
            target.setAudioLinkResolvedAt(source.getAudioLinkResolvedAt());
        }
        if (target.getYoutubeLink() == null) {
            target.setYoutubeLink(source.getYoutubeLink());
        }
        trackRepository.save(target);

        knowledgeRepository.deleteVotesAlsoOnTarget(sourceId, targetId);
        knowledgeRepository.moveVotes(sourceId, targetId);
        trackTagRepository.deleteTagsAlsoOnTarget(sourceId, targetId);
        trackTagRepository.moveTags(sourceId, targetId);
        blindtestTrackRepository.replaceTrack(sourceId, targetId);
        trackRepository.deleteById(sourceId);
    }

    public MergePreview previewGames(Integer sourceId, Integer targetId) {
        requireDifferent(sourceId, targetId);
        Game source = gameRepository.findById(sourceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jeu introuvable"));
        Game target = gameRepository.findById(targetId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Jeu cible inconnu"));
        List<String> clashes = trackRepository.findNamesInBothGames(sourceId, targetId);
        if (!clashes.isEmpty()) {
            return new MergePreview(false, "Musiques présentes dans les deux jeux, à fusionner d'abord : " + String.join(", ", clashes));
        }
        return new MergePreview(true, "« " + source.getName() + " » sera supprimé et son contenu ("
            + plural(trackRepository.countByGameId(sourceId), "musique", "musiques")
            + ") passera dans « " + target.getName() + " ».");
    }

    @Transactional
    public void mergeGames(Integer sourceId, Integer targetId) {
        requireAllowed(previewGames(sourceId, targetId));
        trackRepository.moveTracksToGame(sourceId, targetId);
        gameRepository.deleteById(sourceId);
    }

    public MergePreview previewFranchises(Integer sourceId, Integer targetId) {
        requireDifferent(sourceId, targetId);
        Franchise source = franchiseRepository.findById(sourceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Franchise introuvable"));
        Franchise target = franchiseRepository.findById(targetId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Franchise cible inconnue"));
        List<String> clashes = gameRepository.findNamesInBothFranchises(sourceId, targetId);
        if (!clashes.isEmpty()) {
            return new MergePreview(false, "Jeux présents dans les deux franchises, à fusionner d'abord : " + String.join(", ", clashes));
        }
        return new MergePreview(true, "« " + source.getName() + " » sera supprimée et son contenu ("
            + plural(gameRepository.countByFranchiseId(sourceId), "jeu", "jeux") + ", "
            + plural(trackRepository.countByFranchiseId(sourceId), "musique", "musiques")
            + ") passera dans « " + target.getName() + " ».");
    }

    @Transactional
    public void mergeFranchises(Integer sourceId, Integer targetId) {
        requireAllowed(previewFranchises(sourceId, targetId));
        gameRepository.moveGamesToFranchise(sourceId, targetId);
        franchiseRepository.deleteById(sourceId);
    }

    public MergePreview previewListeners(Integer sourceId, Integer targetId) {
        requireDifferent(sourceId, targetId);
        Listener source = listenerRepository.findById(sourceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pseudo introuvable"));
        Listener target = listenerRepository.findById(targetId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pseudo cible inconnu"));
        StringBuilder summary = new StringBuilder("« " + source.getName() + " » sera supprimé. « " + target.getName()
            + " » récupère " + plural(knowledgeRepository.countByListenerId(sourceId), "vote", "votes")
            + " et " + plural(blindtestScoreRepository.countByListenerId(sourceId), "score", "scores") + " de blindtest.");
        long tracks = knowledgeRepository.countTracksVotedByBoth(sourceId, targetId);
        long blindtests = blindtestScoreRepository.countBlindtestsPlayedByBoth(sourceId, targetId);
        if (tracks > 0 || blindtests > 0) {
            summary.append(" En commun : ").append(plural(tracks, "musique votée", "musiques votées"))
                .append(" et ").append(plural(blindtests, "blindtest joué", "blindtests joués"))
                .append(" : ce sont les votes et scores de « ").append(target.getName()).append(" » qui restent.");
        }
        return new MergePreview(true, summary.toString());
    }

    @Transactional
    public void mergeListeners(Integer sourceId, Integer targetId) {
        previewListeners(sourceId, targetId);
        knowledgeRepository.deleteVotesAlsoByTarget(sourceId, targetId);
        knowledgeRepository.moveVotesToListener(sourceId, targetId);
        blindtestScoreRepository.deleteScoresAlsoOfTarget(sourceId, targetId);
        blindtestScoreRepository.moveScoresToListener(sourceId, targetId);
        listenerRepository.deleteById(sourceId);
    }

    public MergePreview previewTags(Integer sourceId, Integer targetId) {
        requireDifferent(sourceId, targetId);
        Tag source = tagRepository.findById(sourceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag introuvable"));
        Tag target = tagRepository.findById(targetId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tag cible inconnu"));
        StringBuilder summary = new StringBuilder("« " + source.getName() + " » sera supprimé et « " + target.getName()
            + " » sera mis sur ses musiques (" + plural(trackTagRepository.countByTagId(sourceId), "musique", "musiques") + ").");
        long both = trackTagRepository.countTracksTaggedWithBoth(sourceId, targetId);
        if (both > 0) {
            summary.append(" ").append(plural(both, "musique avait", "musiques avaient")).append(" déjà les deux.");
        }
        return new MergePreview(true, summary.toString());
    }

    @Transactional
    public void mergeTags(Integer sourceId, Integer targetId) {
        previewTags(sourceId, targetId);
        trackTagRepository.deleteTracksAlsoTaggedWithTarget(sourceId, targetId);
        trackTagRepository.moveToTag(sourceId, targetId);
        tagRepository.deleteById(sourceId);
    }

    private void requireDifferent(Integer sourceId, Integer targetId) {
        if (targetId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'élément à garder est obligatoire");
        }
        if (targetId.equals(sourceId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossible de fusionner un élément avec lui-même");
        }
    }

    private void requireAllowed(MergePreview preview) {
        if (!preview.allowed()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, preview.summary());
        }
    }

    private static String plural(long count, String singular, String pluralForm) {
        return count + " " + (count > 1 ? pluralForm : singular);
    }
}
