package fr.insalan.blindtest;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;

import fr.insalan.blindtest.model.Franchise;
import fr.insalan.blindtest.model.Game;
import fr.insalan.blindtest.repository.BlindtestDifficultyBandRepository;
import fr.insalan.blindtest.repository.BlindtestRepository;
import fr.insalan.blindtest.repository.BlindtestScoreRepository;
import fr.insalan.blindtest.repository.BlindtestTrackRepository;
import fr.insalan.blindtest.repository.FranchiseRepository;
import fr.insalan.blindtest.repository.GameRepository;
import fr.insalan.blindtest.repository.KnowledgeRepository;
import fr.insalan.blindtest.repository.ListenerRepository;
import fr.insalan.blindtest.repository.TagRepository;
import fr.insalan.blindtest.repository.TrackReportRepository;
import fr.insalan.blindtest.repository.TrackRepository;
import fr.insalan.blindtest.repository.TrackTagRepository;
import fr.insalan.blindtest.security.TokenStore;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class IntegrationTest {

    @Autowired
    private TrackReportRepository trackReportRepository;

    @Autowired
    private BlindtestDifficultyBandRepository blindtestDifficultyBandRepository;

    @Autowired
    private BlindtestScoreRepository blindtestScoreRepository;

    @Autowired
    private BlindtestTrackRepository blindtestTrackRepository;

    @Autowired
    private BlindtestRepository blindtestRepository;

    @Autowired
    private KnowledgeRepository knowledgeRepository;

    @Autowired
    private TrackTagRepository trackTagRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private FranchiseRepository franchiseRepository;

    @Autowired
    private ListenerRepository listenerRepository;

    @Autowired
    private TokenStore tokenStore;

    // Dans l'ordre des clés étrangères. tag_type est une liste fixe (migration V3), on ne la vide pas.
    @AfterEach
    void cleanDatabase() {
        trackReportRepository.deleteAll();
        blindtestDifficultyBandRepository.deleteAll();
        blindtestScoreRepository.deleteAll();
        blindtestTrackRepository.deleteAll();
        blindtestRepository.deleteAll();
        knowledgeRepository.deleteAll();
        trackTagRepository.deleteAll();
        tagRepository.deleteAll();
        trackRepository.deleteAll();
        gameRepository.deleteAll();
        franchiseRepository.deleteAll();
        listenerRepository.deleteAll();
    }

    protected Game stellarBlade() {
        return gameRepository.save(new Game("Stellar Blade", franchiseRepository.save(new Franchise("Stellar Blade"))));
    }

    protected String adminToken() {
        return "Bearer " + tokenStore.create("admin");
    }
}
