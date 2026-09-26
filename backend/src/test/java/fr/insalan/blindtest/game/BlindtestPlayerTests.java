package fr.insalan.blindtest.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import fr.insalan.blindtest.model.Blindtest;
import fr.insalan.blindtest.model.BlindtestScore;
import fr.insalan.blindtest.model.BlindtestScoreId;
import fr.insalan.blindtest.model.BlindtestTrack;
import fr.insalan.blindtest.model.Franchise;
import fr.insalan.blindtest.model.Game;
import fr.insalan.blindtest.model.KnowledgeId;
import fr.insalan.blindtest.model.Listener;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.repository.BlindtestRepository;
import fr.insalan.blindtest.repository.BlindtestScoreRepository;
import fr.insalan.blindtest.repository.BlindtestTrackRepository;
import fr.insalan.blindtest.repository.FranchiseRepository;
import fr.insalan.blindtest.repository.GameRepository;
import fr.insalan.blindtest.repository.KnowledgeRepository;
import fr.insalan.blindtest.repository.ListenerRepository;
import fr.insalan.blindtest.repository.TrackRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class BlindtestPlayerTests {

    @Autowired
    BlindtestPlayer blindtestPlayer;

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

    @Autowired
    BlindtestRepository blindtestRepository;

    @Autowired
    BlindtestTrackRepository blindtestTrackRepository;

    @Autowired
    BlindtestScoreRepository blindtestScoreRepository;

    private Blindtest blindtest;
    private Listener listener;
    private Franchise stellarBladeFranchise;
    private Game stellarBlade;
    private Track dawn;
    private Track shael;

    private void setUp(int trackCount) {
        setUp(trackCount, 5);
    }

    private void setUp(int trackCount, int maxAttempts) {
        stellarBladeFranchise = franchiseRepository.save(new Franchise("Stellar Blade"));
        stellarBlade = gameRepository.save(new Game("Stellar Blade", stellarBladeFranchise));
        blindtest = blindtestRepository.save(new Blindtest("Test", 50, maxAttempts));
        listener = listenerRepository.save(new Listener("Nyx"));

        String[] names = { "Dawn", "Shaël", "Raven", "Democrawler" };
        for (int position = 0; position < trackCount; position++) {
            Track track = trackRepository.save(new Track(names[position], stellarBlade));
            track.setAudioLink("https://example.org/" + names[position]);
            trackRepository.save(track);
            blindtestTrackRepository.save(new BlindtestTrack(blindtest, track, position));
            if (position == 0) {
                dawn = track;
            }
            if (position == 1) {
                shael = track;
            }
        }
    }

    private BlindtestScore score() {
        return blindtestScoreRepository.findById(new BlindtestScoreId(blindtest.getId(), listener.getId())).orElseThrow();
    }

    private boolean knowsDawn() {
        return knowledgeRepository.findById(new KnowledgeId(listener.getId(), dawn.getId())).orElseThrow().isKnows();
    }

    @Test
    void currentTrackIsFirstTrackAndCreatesScore() {
        setUp(2);

        Optional<Track> current = blindtestPlayer.currentTrack(blindtest.getId(), listener.getId());

        assertEquals(dawn, current.orElseThrow());
        assertEquals(0, score().getGoodAnswers());
        assertEquals(0, score().getTracksHeard());
    }

    @Test
    void correctGuessScoresAndAdvances() {
        setUp(2);

        GuessResult result = blindtestPlayer.guess(blindtest.getId(), listener.getId(), stellarBlade.getId(), null);

        assertTrue(result.correct());
        assertEquals(dawn, result.revealed().orElseThrow());
        assertEquals(1, score().getGoodAnswers());
        assertEquals(1, score().getFranchiseAnswers());
        assertEquals(0, score().getBonusAnswers());
        assertEquals(1, score().getTracksHeard());
        assertTrue(knowsDawn());
    }

    @Test
    void findingTheFranchiseFirstThenTheGameDoesNotDoubleCountTheFranchisePoint() {
        setUp(2);

        blindtestPlayer.guessFranchise(blindtest.getId(), listener.getId(), stellarBladeFranchise.getId());
        blindtestPlayer.guess(blindtest.getId(), listener.getId(), stellarBlade.getId(), null);

        assertEquals(1, score().getGoodAnswers());
        assertEquals(1, score().getFranchiseAnswers());
    }

    @Test
    void wrongGuessChangesNothingWhenAttemptsRemain() {
        setUp(2);

        GuessResult result = blindtestPlayer.guess(blindtest.getId(), listener.getId(), stellarBlade.getId() + 1000, null);

        assertFalse(result.correct());
        assertTrue(result.revealed().isEmpty());
        assertEquals(dawn, blindtestPlayer.currentTrack(blindtest.getId(), listener.getId()).orElseThrow());
    }

    @Test
    void correctGameAndTrackAlsoAwardsBonusPoint() {
        setUp(2);

        blindtestPlayer.guess(blindtest.getId(), listener.getId(), stellarBlade.getId(), dawn.getId());

        assertEquals(1, score().getGoodAnswers());
        assertEquals(1, score().getBonusAnswers());
    }

    @Test
    void correctGameButWrongTrackDoesNotAwardBonusPoint() {
        setUp(2);

        blindtestPlayer.guess(blindtest.getId(), listener.getId(), stellarBlade.getId(), shael.getId());

        assertEquals(1, score().getGoodAnswers());
        assertEquals(0, score().getBonusAnswers());
    }

    @Test
    void correctFranchiseAwardsFranchisePointWithoutResolvingTheTrack() {
        setUp(2);

        GuessResult result = blindtestPlayer.guessFranchise(blindtest.getId(), listener.getId(), stellarBladeFranchise.getId());

        assertTrue(result.correct());
        assertTrue(result.revealed().isEmpty());
        assertEquals(1, score().getFranchiseAnswers());
        assertEquals(1, score().getTotalAttempts());
        assertEquals(1, score().getAttemptsUsedOnCurrentTrack());
        assertEquals(0, score().getTracksHeard());
        assertEquals(dawn, blindtestPlayer.currentTrack(blindtest.getId(), listener.getId()).orElseThrow());
    }

    @Test
    void wrongFranchiseChangesNothingButCountsAsAnAttempt() {
        setUp(2);

        GuessResult result = blindtestPlayer.guessFranchise(blindtest.getId(), listener.getId(), stellarBladeFranchise.getId() + 1000);

        assertFalse(result.correct());
        assertEquals(0, score().getFranchiseAnswers());
        assertEquals(1, score().getTotalAttempts());
        assertEquals(1, score().getAttemptsUsedOnCurrentTrack());
    }

    @Test
    void resubmittingTheCorrectFranchiseDoesNotDoubleCountThePoint() {
        setUp(2);

        blindtestPlayer.guessFranchise(blindtest.getId(), listener.getId(), stellarBladeFranchise.getId());
        blindtestPlayer.guessFranchise(blindtest.getId(), listener.getId(), stellarBladeFranchise.getId());

        assertEquals(1, score().getFranchiseAnswers());
        assertEquals(2, score().getTotalAttempts());
    }

    @Test
    void exhaustingAttemptsOnWrongGuessesRevealsTheTrackLikeAPass() {
        setUp(2, 2);

        blindtestPlayer.guess(blindtest.getId(), listener.getId(), stellarBlade.getId() + 1000, null);
        GuessResult result = blindtestPlayer.guess(blindtest.getId(), listener.getId(), stellarBlade.getId() + 1000, null);

        assertFalse(result.correct());
        assertEquals(dawn, result.revealed().orElseThrow());
        assertEquals(0, score().getGoodAnswers());
        assertEquals(1, score().getTracksHeard());
        assertEquals(0, score().getAttemptsUsedOnCurrentTrack());
        assertFalse(knowsDawn());
    }

    @Test
    void exhaustingAttemptsOnFranchiseGuessesAlsoRevealsTheTrack() {
        setUp(2, 1);

        GuessResult result = blindtestPlayer.guessFranchise(blindtest.getId(), listener.getId(), stellarBladeFranchise.getId() + 1000);

        assertFalse(result.correct());
        assertEquals(dawn, result.revealed().orElseThrow());
        assertEquals(1, score().getTracksHeard());
        assertEquals(0, score().getAttemptsUsedOnCurrentTrack());
    }

    @Test
    void attemptCountersResetOnTheNextTrack() {
        setUp(2, 5);

        blindtestPlayer.guessFranchise(blindtest.getId(), listener.getId(), stellarBladeFranchise.getId());
        blindtestPlayer.guess(blindtest.getId(), listener.getId(), stellarBlade.getId(), null);

        assertEquals(0, score().getAttemptsUsedOnCurrentTrack());
        assertFalse(score().isFranchiseFoundOnCurrentTrack());
    }

    @Test
    void passRevealsAndAdvancesWithoutScoring() {
        setUp(2);

        Track revealed = blindtestPlayer.pass(blindtest.getId(), listener.getId());

        assertEquals(dawn, revealed);
        assertEquals(0, score().getGoodAnswers());
        assertEquals(1, score().getTracksHeard());
        assertFalse(knowsDawn());
    }

    @Test
    void knowAnywayCorrectsKnowledgeWithoutTouchingScore() {
        setUp(2);
        blindtestPlayer.pass(blindtest.getId(), listener.getId());

        blindtestPlayer.knowAnyway(listener.getId(), dawn.getId());

        assertTrue(knowsDawn());
        assertEquals(0, score().getGoodAnswers());
        assertEquals(1, score().getTracksHeard());
    }

    @Test
    void currentTrackIsEmptyOnceFinished() {
        setUp(1);

        blindtestPlayer.pass(blindtest.getId(), listener.getId());

        assertTrue(blindtestPlayer.currentTrack(blindtest.getId(), listener.getId()).isEmpty());
    }
}
