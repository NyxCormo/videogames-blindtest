package fr.insalan.blindtest.deletion;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.repository.BlindtestDifficultyBandRepository;
import fr.insalan.blindtest.repository.BlindtestRepository;
import fr.insalan.blindtest.repository.BlindtestScoreRepository;
import fr.insalan.blindtest.repository.BlindtestTrackRepository;
import fr.insalan.blindtest.repository.KnowledgeRepository;
import fr.insalan.blindtest.repository.ListenerRepository;
import fr.insalan.blindtest.repository.TagRepository;
import fr.insalan.blindtest.repository.TrackRepository;
import fr.insalan.blindtest.repository.TrackTagRepository;
import jakarta.transaction.Transactional;

// La base n'a pas de suppression en cascade : on supprime d'abord ce qui pointe vers l'élément, puis l'élément.
@Service
public class DeletionService {

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

    // Une musique déjà votée ou jouée se fusionne : la supprimer ferait perdre ces données.
    @Transactional
    public void deleteTrack(Integer id) {
        Track track = trackRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Musique introuvable"));
        long votes = knowledgeRepository.countByTrackId(id);
        long blindtests = blindtestTrackRepository.countBlindtestsWithTrack(id);
        if (votes > 0 || blindtests > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "« " + track.getName() + " » a " + votes + " vote(s) et apparaît dans " + blindtests
                    + " blindtest(s) : la fusionner plutôt que la supprimer");
        }
        trackTagRepository.deleteByTrackId(id);
        trackRepository.deleteById(id);
    }

    @Transactional
    public void deleteBlindtest(Integer id) {
        if (!blindtestRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Blindtest introuvable");
        }
        blindtestTrackRepository.deleteByBlindtestId(id);
        blindtestScoreRepository.deleteByBlindtestId(id);
        blindtestDifficultyBandRepository.deleteByBlindtestId(id);
        blindtestRepository.deleteById(id);
    }

    @Transactional
    public void deleteListener(Integer id) {
        if (!listenerRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pseudo introuvable");
        }
        knowledgeRepository.deleteByListenerId(id);
        blindtestScoreRepository.deleteByListenerId(id);
        listenerRepository.deleteById(id);
    }

    // Retire le tag de toutes les musiques qui l'avaient, puis le supprime.
    @Transactional
    public void deleteTag(Integer id) {
        if (!tagRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag introuvable");
        }
        trackTagRepository.deleteByTagId(id);
        tagRepository.deleteById(id);
    }
}
