package fr.insalan.blindtest.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.insalan.blindtest.model.TrackTag;
import fr.insalan.blindtest.model.TrackTagId;

public interface TrackTagRepository extends JpaRepository<TrackTag, TrackTagId> {

}
