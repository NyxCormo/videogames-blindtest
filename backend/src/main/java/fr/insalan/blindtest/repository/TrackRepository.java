package fr.insalan.blindtest.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import fr.insalan.blindtest.model.Game;
import fr.insalan.blindtest.model.Track;

public interface TrackRepository extends JpaRepository<Track, Integer> {

    // join fetch charge le jeu et la franchise dans la même requête (sinon Lazy)
    @Query("""
            SELECT t FROM Track t
            JOIN FETCH t.game g
            JOIN FETCH g.franchise f
            ORDER BY f.name, g.name, t.name
            """)
    List<Track> findAllWithGameAndFranchise();

    Optional<Track> findByGameAndName(Game game, String name);
}
