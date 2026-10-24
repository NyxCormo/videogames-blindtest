package fr.insalan.blindtest.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import fr.insalan.blindtest.model.CoverProposal;
import fr.insalan.blindtest.model.CoverProposalId;

public interface CoverProposalRepository extends JpaRepository<CoverProposal, CoverProposalId> {

    @Query("""
            SELECT p FROM CoverProposal p
            JOIN FETCH p.listener
            JOIN FETCH p.game g
            JOIN FETCH g.franchise
            ORDER BY p.createdAt
            """)
    List<CoverProposal> findAllWithGameAndListener();

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM cover_proposal WHERE game_id = :gameId AND image_url IS NULL", nativeQuery = true)
    void deleteWithoutImageByGameId(@Param("gameId") Integer gameId);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM cover_proposal WHERE game_id = :gameId AND image_url = :imageUrl", nativeQuery = true)
    void deleteByGameIdAndImageUrl(@Param("gameId") Integer gameId, @Param("imageUrl") String imageUrl);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM cover_proposal WHERE game_id = :gameId", nativeQuery = true)
    void deleteByGameId(@Param("gameId") Integer gameId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            DELETE FROM cover_proposal
            WHERE game_id = :sourceId
              AND listener_id IN (SELECT listener_id FROM cover_proposal WHERE game_id = :targetId)
            """, nativeQuery = true)
    void deleteProposalsAlsoOnTarget(@Param("sourceId") Integer sourceId, @Param("targetId") Integer targetId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE cover_proposal SET game_id = :targetId WHERE game_id = :sourceId", nativeQuery = true)
    void moveProposals(@Param("sourceId") Integer sourceId, @Param("targetId") Integer targetId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            DELETE FROM cover_proposal
            WHERE listener_id = :sourceId
              AND game_id IN (SELECT game_id FROM cover_proposal WHERE listener_id = :targetId)
            """, nativeQuery = true)
    void deleteProposalsAlsoByTarget(@Param("sourceId") Integer sourceId, @Param("targetId") Integer targetId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE cover_proposal SET listener_id = :targetId WHERE listener_id = :sourceId", nativeQuery = true)
    void moveProposalsToListener(@Param("sourceId") Integer sourceId, @Param("targetId") Integer targetId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM cover_proposal WHERE listener_id = :listenerId", nativeQuery = true)
    void deleteByListenerId(@Param("listenerId") Integer listenerId);
}
