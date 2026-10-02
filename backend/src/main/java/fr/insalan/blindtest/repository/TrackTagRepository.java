package fr.insalan.blindtest.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    // pour chaque tag, le nombre de musiques qui l'ont (les plus utilisés en premier) : id du tag + total
    @Query("""
            SELECT tt.id.tagId, COUNT(tt) FROM TrackTag tt
            GROUP BY tt.id.tagId
            ORDER BY COUNT(tt) DESC
            """)
    List<Object[]> countTracksByTag(Pageable limit);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            DELETE FROM track_tag
            WHERE track_id = :sourceId
              AND tag_id IN (SELECT tag_id FROM track_tag WHERE track_id = :targetId)
            """, nativeQuery = true)
    void deleteTagsAlsoOnTarget(@Param("sourceId") Integer sourceId, @Param("targetId") Integer targetId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE track_tag SET track_id = :targetId WHERE track_id = :sourceId", nativeQuery = true)
    void moveTags(@Param("sourceId") Integer sourceId, @Param("targetId") Integer targetId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM track_tag WHERE track_id = :trackId", nativeQuery = true)
    void deleteByTrackId(@Param("trackId") Integer trackId);

    // Fusion de tags : une musique qui a déjà le tag gardé ne le reçoit pas une deuxième fois.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            DELETE FROM track_tag
            WHERE tag_id = :sourceId
              AND track_id IN (SELECT track_id FROM track_tag WHERE tag_id = :targetId)
            """, nativeQuery = true)
    void deleteTracksAlsoTaggedWithTarget(@Param("sourceId") Integer sourceId, @Param("targetId") Integer targetId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE track_tag SET tag_id = :targetId WHERE tag_id = :sourceId", nativeQuery = true)
    void moveToTag(@Param("sourceId") Integer sourceId, @Param("targetId") Integer targetId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM track_tag WHERE tag_id = :tagId", nativeQuery = true)
    void deleteByTagId(@Param("tagId") Integer tagId);

    @Query(value = "SELECT count(*) FROM track_tag WHERE track_id = :trackId", nativeQuery = true)
    long countByTrackId(@Param("trackId") Integer trackId);

    @Query(value = "SELECT count(*) FROM track_tag WHERE tag_id = :tagId", nativeQuery = true)
    long countByTagId(@Param("tagId") Integer tagId);
}
