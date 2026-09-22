package fr.insalan.blindtest.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.insalan.blindtest.model.Blindtest;

public interface BlindtestRepository extends JpaRepository<Blindtest, Integer> {

    List<Blindtest> findAllByOrderByCreatedAtDesc();
}
