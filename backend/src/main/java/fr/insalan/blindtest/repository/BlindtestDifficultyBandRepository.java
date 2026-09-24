package fr.insalan.blindtest.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.insalan.blindtest.model.BlindtestDifficultyBand;
import fr.insalan.blindtest.model.BlindtestDifficultyBandId;

public interface BlindtestDifficultyBandRepository extends JpaRepository<BlindtestDifficultyBand, BlindtestDifficultyBandId> {

}
