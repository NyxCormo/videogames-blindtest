package fr.insalan.blindtest.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.insalan.blindtest.model.BlindtestScore;
import fr.insalan.blindtest.model.BlindtestScoreId;

public interface BlindtestScoreRepository extends JpaRepository<BlindtestScore, BlindtestScoreId> {

}
