package fr.insalan.blindtest.merge;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
// si une étape échoue, rien n'est modifié.
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

    @Transactional
    public void mergeTracks(Integer sourceId, Integer targetId) {
        requireDifferent(sourceId, targetId);
        Track source = trackRepository.findById(sourceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Musique introuvable"));
        Track target = trackRepository.findById(targetId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Musique cible inconnue"));

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

    @Transactional
    public void mergeGames(Integer sourceId, Integer targetId) {
        requireDifferent(sourceId, targetId);
        if (!gameRepository.existsById(sourceId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Jeu introuvable");
        }
        if (!gameRepository.existsById(targetId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Jeu cible inconnu");
        }
        List<String> clashes = trackRepository.findNamesInBothGames(sourceId, targetId);
        if (!clashes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Musiques présentes dans les deux jeux, à fusionner d'abord : " + String.join(", ", clashes));
        }
        trackRepository.moveTracksToGame(sourceId, targetId);
        gameRepository.deleteById(sourceId);
    }

    @Transactional
    public void mergeFranchises(Integer sourceId, Integer targetId) {
        requireDifferent(sourceId, targetId);
        if (!franchiseRepository.existsById(sourceId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Franchise introuvable");
        }
        if (!franchiseRepository.existsById(targetId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Franchise cible inconnue");
        }
        List<String> clashes = gameRepository.findNamesInBothFranchises(sourceId, targetId);
        if (!clashes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Jeux présents dans les deux franchises, à fusionner d'abord : " + String.join(", ", clashes));
        }
        gameRepository.moveGamesToFranchise(sourceId, targetId);
        franchiseRepository.deleteById(sourceId);
    }

    @Transactional
    public void mergeListeners(Integer sourceId, Integer targetId) {
        requireDifferent(sourceId, targetId);
        if (!listenerRepository.existsById(sourceId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pseudo introuvable");
        }
        if (!listenerRepository.existsById(targetId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pseudo cible inconnu");
        }
        knowledgeRepository.deleteVotesAlsoByTarget(sourceId, targetId);
        knowledgeRepository.moveVotesToListener(sourceId, targetId);
        blindtestScoreRepository.deleteScoresAlsoOfTarget(sourceId, targetId);
        blindtestScoreRepository.moveScoresToListener(sourceId, targetId);
        listenerRepository.deleteById(sourceId);
    }

    @Transactional
    public void mergeTags(Integer sourceId, Integer targetId) {
        requireDifferent(sourceId, targetId);
        if (!tagRepository.existsById(sourceId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag introuvable");
        }
        if (!tagRepository.existsById(targetId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tag cible inconnu");
        }
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
}
