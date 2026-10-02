package fr.insalan.blindtest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fr.insalan.blindtest.model.BlindtestDifficultyBand;
import fr.insalan.blindtest.model.BlindtestDifficultyBandId;

public interface BlindtestDifficultyBandRepository extends JpaRepository<BlindtestDifficultyBand, BlindtestDifficultyBandId> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM blindtest_difficulty_band WHERE blindtest_id = :blindtestId", nativeQuery = true)
    void deleteByBlindtestId(@Param("blindtestId") Integer blindtestId);
}
