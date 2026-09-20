package fr.insalan.blindtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import fr.insalan.blindtest.model.Franchise;
import fr.insalan.blindtest.model.Game;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.repository.FranchiseRepository;
import fr.insalan.blindtest.repository.GameRepository;
import fr.insalan.blindtest.repository.TrackRepository;
import jakarta.persistence.EntityManager;


@SpringBootTest 
@ActiveProfiles("test")
@Transactional 
public class TrackRepositoryTests {
    
    @Autowired 
    FranchiseRepository franchiseRepository;

    @Autowired
    GameRepository gameRepository;

    @Autowired
    TrackRepository trackRepository;

    @Autowired 
    EntityManager entityManager;

    @Autowired 
    JdbcTemplate jdbc;

    @Test
    void saveTrackWithItsGameAndFranchise() {
        Franchise franchise = franchiseRepository.save(new Franchise("Stellar Blade"));
        Game game = gameRepository.save(new Game("Stellar Blade", franchise));
        Track savedTrack = trackRepository.save(new Track("Dawn", game));

        // On vide le cache de JPA
        entityManager.flush();
        entityManager.clear();

        assertNotNull(savedTrack.getId());
        Track foundTrack = trackRepository.findById(savedTrack.getId()).orElseThrow();
        assertEquals("Dawn", foundTrack.getName());
        assertEquals("Stellar Blade", foundTrack.getGame().getName());
        assertEquals("Stellar Blade", foundTrack.getGame().getFranchise().getName());
    }

    @Test
    void resolvedAtIsStoredAsEpochMilliseconds() {
        Game game = gameRepository.save(new Game("Stellar Blade", franchiseRepository.save(new Franchise("Stellar Blade"))));
        Track track = new Track("Dawn", game);
        Instant resolvedAt = Instant.parse("2004-06-24T12:00:00Z");
        track.setAudioLinkResolvedAt(resolvedAt);
        trackRepository.save(track);

        // On vide le cache de JPA
        entityManager.flush();
        entityManager.clear();

        Long stored = jdbc.queryForObject("select audio_link_resolved_at from track", Long.class);
        assertEquals(resolvedAt.toEpochMilli(), stored);
        assertEquals(resolvedAt, trackRepository.findById(track.getId()).orElseThrow().getAudioLinkResolvedAt());
    }
}
