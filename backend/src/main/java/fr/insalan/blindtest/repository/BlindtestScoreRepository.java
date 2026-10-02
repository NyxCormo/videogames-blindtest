package fr.insalan.blindtest.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fr.insalan.blindtest.model.BlindtestScore;
import fr.insalan.blindtest.model.BlindtestScoreId;

public interface BlindtestScoreRepository extends JpaRepository<BlindtestScore, BlindtestScoreId> {

    // join fetch charge le listener dans la même requête (sinon Lazy, et open-in-view=false empêche de le
    // charger plus tard depuis le contrôleur)
    @Query("""
            SELECT bs FROM BlindtestScore bs
            JOIN FETCH bs.listener l
            WHERE bs.id.blindtestId = :blindtestId
            ORDER BY bs.goodAnswers DESC, bs.tracksHeard DESC
            """)
    List<BlindtestScore> findWithListenerByBlindtestId(@Param("blindtestId") Integer blindtestId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM blindtest_score WHERE blindtest_id = :blindtestId", nativeQuery = true)
    void deleteByBlindtestId(@Param("blindtestId") Integer blindtestId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM blindtest_score WHERE listener_id = :listenerId", nativeQuery = true)
    void deleteByListenerId(@Param("listenerId") Integer listenerId);

    // Fusion de pseudos : si les deux ont joué le même blindtest, le score du pseudo gardé l'emporte.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            DELETE FROM blindtest_score
            WHERE listener_id = :sourceId
              AND blindtest_id IN (SELECT blindtest_id FROM blindtest_score WHERE listener_id = :targetId)
            """, nativeQuery = true)
    void deleteScoresAlsoOfTarget(@Param("sourceId") Integer sourceId, @Param("targetId") Integer targetId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE blindtest_score SET listener_id = :targetId WHERE listener_id = :sourceId", nativeQuery = true)
    void moveScoresToListener(@Param("sourceId") Integer sourceId, @Param("targetId") Integer targetId);
}
