package fr.insalan.blindtest.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
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
}
