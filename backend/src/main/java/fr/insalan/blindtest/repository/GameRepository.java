package fr.insalan.blindtest.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.insalan.blindtest.model.Game;

public interface GameRepository extends JpaRepository<Game, Integer> {
}
