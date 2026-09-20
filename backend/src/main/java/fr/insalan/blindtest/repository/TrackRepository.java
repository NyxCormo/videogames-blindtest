package fr.insalan.blindtest.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.insalan.blindtest.model.Track;

public interface TrackRepository extends JpaRepository<Track, Integer> {
}
