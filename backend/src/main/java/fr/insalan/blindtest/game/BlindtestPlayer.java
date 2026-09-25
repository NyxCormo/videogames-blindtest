package fr.insalan.blindtest.game;

import java.util.Optional;

import org.springframework.stereotype.Service;

import fr.insalan.blindtest.model.Blindtest;
import fr.insalan.blindtest.model.BlindtestScore;
import fr.insalan.blindtest.model.BlindtestScoreId;
import fr.insalan.blindtest.model.BlindtestTrack;
import fr.insalan.blindtest.model.Knowledge;
import fr.insalan.blindtest.model.Listener;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.repository.BlindtestRepository;
import fr.insalan.blindtest.repository.BlindtestScoreRepository;
import fr.insalan.blindtest.repository.BlindtestTrackRepository;
import fr.insalan.blindtest.repository.KnowledgeRepository;
import fr.insalan.blindtest.repository.ListenerRepository;
import fr.insalan.blindtest.repository.TrackRepository;
import jakarta.transaction.Transactional;


// Fait avancer une partie de blindtest (quelle musique jouer, traitement des réponses)
@Service 
public class BlindtestPlayer {
    
    private final BlindtestRepository blindtestRepository;
    private final BlindtestTrackRepository blindtestTrackRepository;
    private final BlindtestScoreRepository blindtestScoreRepository;
    private final KnowledgeRepository knowledgeRepository;
    private final ListenerRepository listenerRepository;
    private final TrackRepository trackRepository;

    public BlindtestPlayer(
        BlindtestRepository blindtestRepository,
        BlindtestTrackRepository blindtestTrackRepository,
        BlindtestScoreRepository blindtestScoreRepository,
        KnowledgeRepository knowledgeRepository,
        ListenerRepository listenerRepository,
        TrackRepository trackRepository
    ) {
        this.blindtestRepository = blindtestRepository;
        this.blindtestTrackRepository = blindtestTrackRepository;
        this.blindtestScoreRepository = blindtestScoreRepository;
        this.knowledgeRepository = knowledgeRepository;
        this.listenerRepository = listenerRepository;
        this.trackRepository = trackRepository;
    }

    // Musique à jouer pour ce joueur : celle à la position "tracksHeard" de son score.
    // Si la partie est finie : vide.
    @Transactional
    public Optional<Track> currentTrack(Integer blindtestId, Integer listenerId) {
        BlindtestScore score = score(blindtestId, listenerId);
        return blindtestTrackRepository.findWithTrackByBlindtestIdAndPosition(blindtestId, score.getTracksHeard())
            .map(BlindtestTrack::getTrack);
    }

    // Le joueur ne connaît que la franchise : point de franchise acquis une seule fois par musique
    // (une deuxième soumission correcte ne compte pas deux fois), la musique n'est pas résolue pour autant.
    @Transactional
    public boolean guessFranchise(Integer blindtestId, Integer listenerId, Integer franchiseId) {
        BlindtestScore score = score(blindtestId, listenerId);
        Track track = trackAt(blindtestId, score.getTracksHeard());

        boolean correct = track.getGame().getFranchise().getId().equals(franchiseId);
        if (correct && !score.isFranchiseFoundOnCurrentTrack()) {
            score.setFranchiseAnswers(score.getFranchiseAnswers() + 1);
            score.setFranchiseFoundOnCurrentTrack(true);
        }
        score.setTotalAttempts(score.getTotalAttempts() + 1);
        score.setAttemptsUsedOnCurrentTrack(score.getAttemptsUsedOnCurrentTrack() + 1);
        blindtestScoreRepository.save(score);
        return correct;
    }

    // Le joueur choisit un jeu (id) dans une liste, jamais du texte libre.
    // Bonne réponse : connaissance enregistrée, point marqué, musique suivante.
    // Mauvaise réponse : rien ne change (nombre d'essais infini).
    // En plus, s'il a aussi choisi la bonne musique (trackId, optionnel) : point bonus à part, sans effet sur goodAnswers.
    @Transactional
    public Optional<Track> guess(Integer blindtestId, Integer listenerId, Integer gameId, Integer trackId) {
        BlindtestScore score = score(blindtestId, listenerId);
        Track track = trackAt(blindtestId, score.getTracksHeard());

        boolean correct = track.getGame().getId().equals(gameId);
        if (!correct) {
            return Optional.empty();
        }

        recordKnowledge(listenerId, track, true);
        score.setGoodAnswers(score.getGoodAnswers() + 1);
        if (trackId != null && track.getId().equals(trackId)) {
            score.setBonusAnswers(score.getBonusAnswers() + 1);
        }
        score.setTracksHeard(score.getTracksHeard() + 1);
        blindtestScoreRepository.save(score);
        return Optional.of(track);
    }

    // Le joueur passe : la musique est révélée et comptée comme non connue, mais compte quand même comme écoutée.
    @Transactional
    public Track pass(Integer blindtestId, Integer listenerId) {
        BlindtestScore score = score(blindtestId, listenerId);
        Track track = trackAt(blindtestId, score.getTracksHeard());

        recordKnowledge(listenerId, track, false);
        score.setTracksHeard(score.getTracksHeard() + 1);
        blindtestScoreRepository.save(score);
        return track;
    }

    // "Je le savais" après un passe : corrige knowledge, sans toucher au score (déjà décompté par pass()).
    @Transactional
    public void knowAnyway(Integer listenerId, Integer trackId) {
        Track track = trackRepository.findById(trackId).orElseThrow();
        recordKnowledge(listenerId, track, true);
    }

        // save met à jour le vote s'il existe déjà, sinon il le crée (même principe que l'import du Google Sheet)
    private void recordKnowledge(Integer listenerId, Track track, boolean knows) {
        Listener listener = listenerRepository.findById(listenerId).orElseThrow();
        knowledgeRepository.save(new Knowledge(listener, track, knows));
    }

    private Track trackAt(Integer blindtestId, int position) {
        return blindtestTrackRepository.findWithTrackByBlindtestIdAndPosition(blindtestId, position)
            .map(BlindtestTrack::getTrack)
            .orElseThrow(() -> new IllegalStateException("Ce blindtest est déjà terminé pour ce joueur"));
    }

    // Score courant du joueur pour ce blindtest (créé à la volée s'il n'existe pas encore).
    public BlindtestScore score(Integer blindtestId, Integer listenerId) {
        return blindtestScoreRepository.findById(new BlindtestScoreId(blindtestId, listenerId))
            .orElseGet(() -> {
                Blindtest blindtest = blindtestRepository.findById(blindtestId).orElseThrow();
                Listener listener = listenerRepository.findById(listenerId).orElseThrow();
                return blindtestScoreRepository.save(new BlindtestScore(blindtest, listener));
            });
    }
}