package fr.insalan.blindtest.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fr.insalan.blindtest.model.BlindtestTrack;
import fr.insalan.blindtest.model.BlindtestTrackId;

public interface BlindtestTrackRepository extends JpaRepository<BlindtestTrack, BlindtestTrackId> {

    long countByIdBlindtestId(Integer blindtestId);

    // join fetch charge la musique, le jeu et la franchise dans la même requête (sinon Lazy, et open-in-view=false
    // empêche de les charger plus tard depuis le contrôleur)
    @Query("""
            SELECT bt FROM BlindtestTrack bt
            JOIN FETCH bt.track t
            JOIN FETCH t.game g
            JOIN FETCH g.franchise f
            WHERE bt.id.blindtestId = :blindtestId AND bt.id.position = :position
            """)
    Optional<BlindtestTrack> findWithTrackByBlindtestIdAndPosition(
        @Param("blindtestId") Integer blindtestId,
        @Param("position") int position
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE blindtest_track SET track_id = :targetId WHERE track_id = :sourceId", nativeQuery = true)
    void replaceTrack(@Param("sourceId") Integer sourceId, @Param("targetId") Integer targetId);

    @Query(value = "SELECT count(DISTINCT blindtest_id) FROM blindtest_track WHERE track_id = :trackId", nativeQuery = true)
    long countBlindtestsWithTrack(@Param("trackId") Integer trackId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM blindtest_track WHERE blindtest_id = :blindtestId", nativeQuery = true)
    void deleteByBlindtestId(@Param("blindtestId") Integer blindtestId);

    @Query(value = "SELECT count(*) FROM blindtest_track WHERE blindtest_id = :blindtestId", nativeQuery = true)
    long countByBlindtestId(@Param("blindtestId") Integer blindtestId);
}
