package fr.insalan.blindtest.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // même chose que findAllWithGameAndFranchise, mais pour une seule musique
    @Query("""
            SELECT t FROM Track t
            JOIN FETCH t.game g
            JOIN FETCH g.franchise f
            WHERE t.id = :id
            """)
    Optional<Track> findByIdWithGameAndFranchise(@Param("id") Integer id);

    // même chose, mais pour toutes les musiques d'un même jeu (bouton "appliquer au jeu", liste latérale)
    @Query("""
            SELECT t FROM Track t
            JOIN FETCH t.game g
            JOIN FETCH g.franchise f
            WHERE g.id = :gameId
            ORDER BY t.name
            """)
    List<Track> findByGameIdWithGameAndFranchise(@Param("gameId") Integer gameId);

    Optional<Track> findByGameAndName(Game game, String name);

    boolean existsByGameAndNameIgnoreCase(Game game, String name);

    boolean existsByGameAndNameIgnoreCaseAndIdNot(Game game, String name, Integer id);

    long countByGameId(Integer gameId);

    @Query(value = "SELECT count(*) FROM track t JOIN game g ON g.id = t.game_id WHERE g.franchise_id = :franchiseId", nativeQuery = true)
    long countByFranchiseId(@Param("franchiseId") Integer franchiseId);

    Optional<Track> findFirstByKhinsiderLink(String khinsiderLink);

    Optional<Track> findFirstByYoutubeLink(String youtubeLink);

    List<Track> findByKhinsiderLinkIsNotNull();

    @Query("""
            SELECT t FROM Track t
            JOIN FETCH t.game g
            JOIN FETCH g.franchise f
            WHERE t.audioLink IS NOT NULL
            AND t.id NOT IN (SELECT k.track.id FROM Knowledge k WHERE k.listener.id = :listenerId)
            """)
    List<Track> findPlayableUnknownByListener(@Param("listenerId") Integer listenerId);

    @Query(value = """
            SELECT s.name FROM track s
            JOIN track t ON lower(s.name) = lower(t.name)
            WHERE s.game_id = :sourceId AND t.game_id = :targetId
            ORDER BY s.name
            """, nativeQuery = true)
    List<String> findNamesInBothGames(@Param("sourceId") Integer sourceId, @Param("targetId") Integer targetId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE track SET game_id = :targetId WHERE game_id = :sourceId", nativeQuery = true)
    void moveTracksToGame(@Param("sourceId") Integer sourceId, @Param("targetId") Integer targetId);
}
