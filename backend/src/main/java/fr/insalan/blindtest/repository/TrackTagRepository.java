package fr.insalan.blindtest.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fr.insalan.blindtest.model.TrackTag;
import fr.insalan.blindtest.model.TrackTagId;

public interface TrackTagRepository extends JpaRepository<TrackTag, TrackTagId> {

    // join fetch charge le tag et son type dans la même requête (sinon Lazy, et open-in-view=false empêche de
    // les charger plus tard depuis le contrôleur)
    @Query("""
            SELECT tt FROM TrackTag tt
            JOIN FETCH tt.tag t
            JOIN FETCH t.type
            WHERE tt.id.trackId = :trackId
            ORDER BY t.name
            """)
    List<TrackTag> findWithTagByTrackId(@Param("trackId") Integer trackId);
}
