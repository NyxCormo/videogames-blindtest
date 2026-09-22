package fr.insalan.blindtest.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.insalan.blindtest.model.Blindtest;

public interface BlindtestRepository extends JpaRepository<Blindtest, Integer> {

}
