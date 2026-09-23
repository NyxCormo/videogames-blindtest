package fr.insalan.blindtest.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
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
}
