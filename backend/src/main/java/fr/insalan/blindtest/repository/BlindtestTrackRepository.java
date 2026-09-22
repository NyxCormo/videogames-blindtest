package fr.insalan.blindtest.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.insalan.blindtest.model.BlindtestTrack;
import fr.insalan.blindtest.model.BlindtestTrackId;

public interface BlindtestTrackRepository extends JpaRepository<BlindtestTrack, BlindtestTrackId> {

}
