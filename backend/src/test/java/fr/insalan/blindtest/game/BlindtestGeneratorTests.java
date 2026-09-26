package fr.insalan.blindtest.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

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
import fr.insalan.blindtest.model.Tag;
import fr.insalan.blindtest.model.TagType;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.model.TrackTag;
import fr.insalan.blindtest.repository.BlindtestTrackRepository;
import fr.insalan.blindtest.repository.FranchiseRepository;
import fr.insalan.blindtest.repository.GameRepository;
import fr.insalan.blindtest.repository.KnowledgeRepository;
import fr.insalan.blindtest.repository.ListenerRepository;
import fr.insalan.blindtest.repository.TagRepository;
import fr.insalan.blindtest.repository.TagTypeRepository;
import fr.insalan.blindtest.repository.TrackRepository;
import fr.insalan.blindtest.repository.TrackTagRepository;

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

    @Autowired
    TagRepository tagRepository;

    @Autowired
    TagTypeRepository tagTypeRepository;

    @Autowired
    TrackTagRepository trackTagRepository;

    private static final List<DifficultyBand> ANY_DIFFICULTY = List.of(new DifficultyBand(0, 100, 100));

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
    void picksOnlyTracksWithinTheRequestedDifficultyBand() {
        Franchise franchise = franchiseRepository.save(new Franchise("Stellar Blade"));
        Game game = gameRepository.save(new Game("Stellar Blade", franchise));
        playableTrack(game, "Dawn", 3, 3); // ratio 1.0, difficulté 0
        Track shael = playableTrack(game, "Shaël", 0, 3); // ratio 0.0, difficulté 100

        Blindtest blindtest = blindtestGenerator.generate(
            "Test", 1, List.of(new DifficultyBand(90, 100, 100)), List.of(), true, null, null, GenerationStrategy.RANDOM, 5
        );

        assertEquals(List.of(shael), tracksOf(blindtest));
    }

    @Test
    void tracksWithoutVotesAreIgnored() {
        Franchise franchise = franchiseRepository.save(new Franchise("Stellar Blade"));
        Game game = gameRepository.save(new Game("Stellar Blade", franchise));
        Track raven = trackRepository.save(new Track("Raven", game));
        raven.setAudioLink("https://example.org/raven");
        trackRepository.save(raven);

        assertThrows(IllegalStateException.class,
            () -> blindtestGenerator.generate("Test", 1, ANY_DIFFICULTY, List.of(), true, null, null, GenerationStrategy.RANDOM, 5));
    }

    @Test
    void tracksWithoutAudioLinkAreIgnored() {
        Franchise franchise = franchiseRepository.save(new Franchise("Stellar Blade"));
        Game game = gameRepository.save(new Game("Stellar Blade", franchise));
        Track track = trackRepository.save(new Track("Democrawler", game));
        Listener listener = listenerRepository.save(new Listener("Nyx"));
        knowledgeRepository.save(new Knowledge(listener, track, true));

        assertThrows(IllegalStateException.class,
            () -> blindtestGenerator.generate("Test", 1, ANY_DIFFICULTY, List.of(), true, null, null, GenerationStrategy.RANDOM, 5));
    }

    @Test
    void matchAllTagsRestrictsToTracksHavingEveryTag() {
        Franchise franchise = franchiseRepository.save(new Franchise("Stellar Blade"));
        Game game = gameRepository.save(new Game("Stellar Blade", franchise));
        Track dawn = playableTrack(game, "Dawn", 3, 3);
        Track raven = playableTrack(game, "Raven", 0, 3);

        TagType genre = tagTypeRepository.findByName("genre").orElseThrow();
        Tag action = tagRepository.save(new Tag("Action", genre));
        Tag epique = tagRepository.save(new Tag("Épique", genre));
        trackTagRepository.save(new TrackTag(dawn, action));
        trackTagRepository.save(new TrackTag(dawn, epique));
        trackTagRepository.save(new TrackTag(raven, action)); // seulement Action, pas Épique

        Blindtest blindtest = blindtestGenerator.generate(
            "Test", 1, ANY_DIFFICULTY, List.of(action.getId(), epique.getId()), true, null, null, GenerationStrategy.RANDOM, 5
        );

        assertEquals(List.of(dawn), tracksOf(blindtest));
    }

    @Test
    void matchAnyTagWidensToTracksHavingAtLeastOneTag() {
        Franchise franchise = franchiseRepository.save(new Franchise("Stellar Blade"));
        Game game = gameRepository.save(new Game("Stellar Blade", franchise));
        Track dawn = playableTrack(game, "Dawn", 3, 3);
        Track raven = playableTrack(game, "Raven", 0, 3);
        Track shael = playableTrack(game, "Shaël", 0, 3); // pas de tag du tout

        TagType genre = tagTypeRepository.findByName("genre").orElseThrow();
        Tag action = tagRepository.save(new Tag("Action", genre));
        Tag epique = tagRepository.save(new Tag("Épique", genre));
        trackTagRepository.save(new TrackTag(dawn, epique));
        trackTagRepository.save(new TrackTag(raven, action));

        Blindtest blindtest = blindtestGenerator.generate(
            "Test", 2, ANY_DIFFICULTY, List.of(action.getId(), epique.getId()), false, null, null, GenerationStrategy.RANDOM, 5
        );

        List<Track> picked = tracksOf(blindtest);
        assertEquals(2, picked.size());
        assertTrue(picked.contains(dawn));
        assertTrue(picked.contains(raven));
        assertFalse(picked.contains(shael));
    }

    @Test
    void tracksWithoutAnyRequestedTagAreExcludedEvenIfNotEnoughRemain() {
        Franchise franchise = franchiseRepository.save(new Franchise("Stellar Blade"));
        Game game = gameRepository.save(new Game("Stellar Blade", franchise));
        Track dawn = playableTrack(game, "Dawn", 3, 3);
        playableTrack(game, "Raven", 0, 3); // pas taguée

        TagType genre = tagTypeRepository.findByName("genre").orElseThrow();
        Tag action = tagRepository.save(new Tag("Action", genre));
        trackTagRepository.save(new TrackTag(dawn, action));

        assertThrows(IllegalStateException.class, () -> blindtestGenerator.generate(
            "Test", 2, ANY_DIFFICULTY, List.of(action.getId()), true, null, null, GenerationStrategy.RANDOM, 5
        ));
    }

    @Test
    void bandQuotasSumExactlyToTrackCountEvenWithUnevenProportions() {
        Franchise franchise = franchiseRepository.save(new Franchise("Stellar Blade"));
        Game game = gameRepository.save(new Game("Stellar Blade", franchise));
        for (int i = 0; i < 10; i++) {
            playableTrack(game, "Track" + i, 5, 10); // ratio 0.5, tombe dans n'importe quel palier large
        }

        List<DifficultyBand> bands = List.of(
            new DifficultyBand(0, 100, 33),
            new DifficultyBand(0, 100, 33),
            new DifficultyBand(0, 100, 34)
        );

        Blindtest blindtest = blindtestGenerator.generate("Test", 10, bands, List.of(), true, null, null, GenerationStrategy.RANDOM, 5);

        assertEquals(10, tracksOf(blindtest).size());
    }

    @Test
    void maxPerGameCapPreventsExceedingIt() {
        Franchise franchise = franchiseRepository.save(new Franchise("Stellar Blade"));
        Game game = gameRepository.save(new Game("Stellar Blade", franchise));
        playableTrack(game, "Dawn", 0, 3);
        playableTrack(game, "Raven", 0, 3);
        playableTrack(game, "Shaël", 0, 3);
        // 3 musiques du même jeu, mais le plafond n'en autorise que 2

        assertThrows(IllegalStateException.class, () -> blindtestGenerator.generate(
            "Test", 3, ANY_DIFFICULTY, List.of(), true, 2, null, GenerationStrategy.RANDOM, 5
        ));
    }

    @Test
    void maxPerFranchiseCapPreventsExceedingIt() {
        Franchise franchise = franchiseRepository.save(new Franchise("Stellar Blade"));
        Game gameA = gameRepository.save(new Game("Stellar Blade", franchise));
        Game gameB = gameRepository.save(new Game("Stellar Blade: Blood Rain", franchise));
        playableTrack(gameA, "Dawn", 0, 3);
        playableTrack(gameA, "Raven", 0, 3);
        playableTrack(gameB, "Trailer", 0, 3);
        // 3 musiques dans la même franchise (2 jeux), mais le plafond de franchise n'en autorise que 2

        assertThrows(IllegalStateException.class, () -> blindtestGenerator.generate(
            "Test", 3, ANY_DIFFICULTY, List.of(), true, null, 2, GenerationStrategy.RANDOM, 5
        ));
    }

    @Test
    void rareGamesStrategyPrioritizesGamesNeededByFewerBands() {
        Franchise franchise = franchiseRepository.save(new Franchise("Stellar Blade"));
        Game common = gameRepository.save(new Game("Common Game", franchise)); // a une piste dans les deux paliers
        Game rare = gameRepository.save(new Game("Rare Game", franchise)); // seulement le palier facile

        Track rareEasy = playableTrack(rare, "Rare Easy", 3, 3); // ratio 1.0
        playableTrack(common, "Common Easy", 3, 3); // ratio 1.0
        Track commonHard = playableTrack(common, "Common Hard", 0, 3); // ratio 0.0

        List<DifficultyBand> bands = List.of(
            new DifficultyBand(0, 10, 50),
            new DifficultyBand(90, 100, 50)
        );

        Blindtest blindtest = blindtestGenerator.generate(
            "Test", 2, bands, List.of(), true, 1, null, GenerationStrategy.RARE_GAMES, 5
        );

        // Le palier facile doit prendre le jeu rare (fréquence 1) plutôt que le commun (fréquence 2),
        // pour laisser le jeu commun disponible pour le palier difficile qui n'a que lui.
        assertEquals(Set.of(rareEasy, commonHard), Set.copyOf(tracksOf(blindtest)));
    }

    @Test
    void rareFranchisesStrategyPrioritizesFranchisesNeededByFewerBands() {
        Franchise common = franchiseRepository.save(new Franchise("Common Franchise"));
        Franchise rare = franchiseRepository.save(new Franchise("Rare Franchise"));
        Game commonGame = gameRepository.save(new Game("Common Game", common));
        Game rareGame = gameRepository.save(new Game("Rare Game", rare));

        Track rareEasy = playableTrack(rareGame, "Rare Easy", 3, 3);
        playableTrack(commonGame, "Common Easy", 3, 3);
        Track commonHard = playableTrack(commonGame, "Common Hard", 0, 3);

        List<DifficultyBand> bands = List.of(
            new DifficultyBand(0, 10, 50),
            new DifficultyBand(90, 100, 50)
        );

        Blindtest blindtest = blindtestGenerator.generate(
            "Test", 2, bands, List.of(), true, null, 1, GenerationStrategy.RARE_FRANCHISES, 5
        );

        assertEquals(Set.of(rareEasy, commonHard), Set.copyOf(tracksOf(blindtest)));
    }

    @Test
    void automaticEscalationFindsTheOnlyFeasibleSelectionWithoutAnExplicitStrategy() {
        Franchise franchise = franchiseRepository.save(new Franchise("Stellar Blade"));
        Game common = gameRepository.save(new Game("Common Game", franchise));
        Game rare = gameRepository.save(new Game("Rare Game", franchise));

        Track rareEasy = playableTrack(rare, "Rare Easy", 3, 3);
        playableTrack(common, "Common Easy", 3, 3);
        Track commonHard = playableTrack(common, "Common Hard", 0, 3);

        List<DifficultyBand> bands = List.of(
            new DifficultyBand(0, 10, 50),
            new DifficultyBand(90, 100, 50)
        );

        // Sans stratégie imposée (null) : [rareEasy, commonHard] est la seule combinaison possible avec ce
        // plafond, que ce soit trouvé dès le tirage au hasard (par chance) ou grâce au repli "jeux rares".
        Blindtest blindtest = blindtestGenerator.generate("Test", 2, bands, List.of(), true, 1, null, null, 5);

        assertEquals(Set.of(rareEasy, commonHard), Set.copyOf(tracksOf(blindtest)));
    }
}
