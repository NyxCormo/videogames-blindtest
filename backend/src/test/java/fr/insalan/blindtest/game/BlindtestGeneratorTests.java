package fr.insalan.blindtest.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import fr.insalan.blindtest.model.Blindtest;
import fr.insalan.blindtest.model.Franchise;
import fr.insalan.blindtest.model.Game;
import fr.insalan.blindtest.model.Knowledge;
import fr.insalan.blindtest.model.Listener;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.repository.BlindtestTrackRepository;
import fr.insalan.blindtest.repository.FranchiseRepository;
import fr.insalan.blindtest.repository.GameRepository;
import fr.insalan.blindtest.repository.KnowledgeRepository;
import fr.insalan.blindtest.repository.ListenerRepository;
import fr.insalan.blindtest.repository.TrackRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class BlindtestGeneratorTests {

    @Autowired
    BlindtestGenerator blindtestGenerator;

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
    BlindtestTrackRepository blindtestTrackRepository;

    private Track playableTrack(Game game, String name, int knownVotes, int totalVotes) {
        Track track = trackRepository.save(new Track(name, game));
        track.setAudioLink("https://example.org/" + name);
        trackRepository.save(track);

        for (int i = 0; i < totalVotes; i++) {
            Listener listener = listenerRepository.save(new Listener(name + "-listener-" + i));
            knowledgeRepository.save(new Knowledge(listener, track, i < knownVotes));
        }
        return track;
    }

    private List<Track> tracksOf(Blindtest blindtest) {
        return blindtestTrackRepository.findAll().stream()
            .filter(blindtestTrack -> blindtestTrack.getBlindtest().getId().equals(blindtest.getId()))
            .map(blindtestTrack -> blindtestTrack.getTrack())
            .toList();
    }

    @Test
    void pickHardestTrackAtMaxDifficulty() {
        Franchise franchise = franchiseRepository.save(new Franchise("Stellar Blade"));
        Game game = gameRepository.save(new Game("Stellar Blade", franchise));
        playableTrack(game, "Dawn", 3, 3); // ratio 1.0, facile
        Track shael = playableTrack(game, "Shaël", 0, 3); // ratio 0.0, difficile

        Blindtest blindtest = blindtestGenerator.generate("Test", 1, 100);

        assertEquals(List.of(shael), tracksOf(blindtest));
    }

    @Test
    void pickEasiestTrackAtMinDifficulty() {
        Franchise franchise = franchiseRepository.save(new Franchise("Stellar Blade"));
        Game game = gameRepository.save(new Game("Stellar Blade", franchise));
        Track dawn = playableTrack(game, "Dawn", 3, 3);
        playableTrack(game, "Shaël", 0, 3);

        Blindtest blindtest = blindtestGenerator.generate("Test", 1, 0);

        assertEquals(List.of(dawn), tracksOf(blindtest));
    }

    @Test
    void tracksWithoutVotesAreIgnored() {
        Franchise franchise = franchiseRepository.save(new Franchise("Stellar Blade"));
        Game game = gameRepository.save(new Game("Stellar Blade", franchise));
        Track raven = trackRepository.save(new Track("Raven", game));
        raven.setAudioLink("https://example.org/raven");
        trackRepository.save(raven);

        assertThrows(IllegalStateException.class, () -> blindtestGenerator.generate("Test", 1, 50));
    }

    @Test
    void tracksWithoutAudioLinkAreIgnored() {
        Franchise franchise = franchiseRepository.save(new Franchise("Stellar Blade"));
        Game game = gameRepository.save(new Game("Stellar Blade", franchise));
        Track track = trackRepository.save(new Track("Democrawler", game));
        Listener listener = listenerRepository.save(new Listener("Nyx"));
        knowledgeRepository.save(new Knowledge(listener, track, true));

        assertThrows(IllegalStateException.class, () -> blindtestGenerator.generate("Test", 1, 0));
    }
}
