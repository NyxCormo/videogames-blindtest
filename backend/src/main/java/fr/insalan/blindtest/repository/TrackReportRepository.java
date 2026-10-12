package fr.insalan.blindtest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fr.insalan.blindtest.model.TrackReport;
import fr.insalan.blindtest.model.TrackReportId;

public interface TrackReportRepository extends JpaRepository<TrackReport, TrackReportId> {

    // Fusion de musiques : un pseudo qui a signalé les deux ne garde qu'un signalement.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            DELETE FROM track_report
            WHERE track_id = :sourceId
              AND listener_id IN (SELECT listener_id FROM track_report WHERE track_id = :targetId)
            """, nativeQuery = true)
    void deleteReportsAlsoOnTarget(@Param("sourceId") Integer sourceId, @Param("targetId") Integer targetId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE track_report SET track_id = :targetId WHERE track_id = :sourceId", nativeQuery = true)
    void moveReports(@Param("sourceId") Integer sourceId, @Param("targetId") Integer targetId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM track_report WHERE track_id = :trackId", nativeQuery = true)
    void deleteByTrackId(@Param("trackId") Integer trackId);

    // Fusion de pseudos : même règle, vue du côté du pseudo.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            DELETE FROM track_report
            WHERE listener_id = :sourceId
              AND track_id IN (SELECT track_id FROM track_report WHERE listener_id = :targetId)
            """, nativeQuery = true)
    void deleteReportsAlsoByTarget(@Param("sourceId") Integer sourceId, @Param("targetId") Integer targetId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE track_report SET listener_id = :targetId WHERE listener_id = :sourceId", nativeQuery = true)
    void moveReportsToListener(@Param("sourceId") Integer sourceId, @Param("targetId") Integer targetId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM track_report WHERE listener_id = :listenerId", nativeQuery = true)
    void deleteByListenerId(@Param("listenerId") Integer listenerId);
}
