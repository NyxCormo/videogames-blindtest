package fr.insalan.blindtest.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.insalan.blindtest.model.Franchise;
import fr.insalan.blindtest.model.Game;

public interface GameRepository extends JpaRepository<Game, Integer> {

    Optional<Game> findByFranchiseAndName(Franchise franchise, String name);

    List<Game> findByFranchiseIdOrderByNameAsc(Integer franchiseId);
}
