package fr.insalan.blindtest.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fr.insalan.blindtest.model.Knowledge;
import fr.insalan.blindtest.model.KnowledgeId;

public interface KnowledgeRepository extends JpaRepository<Knowledge, KnowledgeId> {

    @Query("SELECT k FROM Knowledge k WHERE k.listener.id = :listenerId")
    List<Knowledge> findByListenerId(@Param("listenerId") Integer listenerId);

    // Fusion de musiques : un vote déjà présent sur la musique gardée l'emporte sur celui de la musique supprimée.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            DELETE FROM knowledge
            WHERE track_id = :sourceId
              AND listener_id IN (SELECT listener_id FROM knowledge WHERE track_id = :targetId)
            """, nativeQuery = true)
    void deleteVotesAlsoOnTarget(@Param("sourceId") Integer sourceId, @Param("targetId") Integer targetId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE knowledge SET track_id = :targetId WHERE track_id = :sourceId", nativeQuery = true)
    void moveVotes(@Param("sourceId") Integer sourceId, @Param("targetId") Integer targetId);

    @Query(value = "SELECT count(*) FROM knowledge WHERE track_id = :trackId", nativeQuery = true)
    long countByTrackId(@Param("trackId") Integer trackId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM knowledge WHERE listener_id = :listenerId", nativeQuery = true)
    void deleteByListenerId(@Param("listenerId") Integer listenerId);

    // Fusion de pseudos : un vote déjà présent pour le pseudo gardé l'emporte sur celui du pseudo supprimé.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            DELETE FROM knowledge
            WHERE listener_id = :sourceId
              AND track_id IN (SELECT track_id FROM knowledge WHERE listener_id = :targetId)
            """, nativeQuery = true)
    void deleteVotesAlsoByTarget(@Param("sourceId") Integer sourceId, @Param("targetId") Integer targetId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE knowledge SET listener_id = :targetId WHERE listener_id = :sourceId", nativeQuery = true)
    void moveVotesToListener(@Param("sourceId") Integer sourceId, @Param("targetId") Integer targetId);

    @Query(value = "SELECT count(*) FROM knowledge WHERE listener_id = :listenerId", nativeQuery = true)
    long countByListenerId(@Param("listenerId") Integer listenerId);
}
