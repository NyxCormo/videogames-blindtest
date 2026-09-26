package fr.insalan.blindtest.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import fr.insalan.blindtest.model.Franchise;
import fr.insalan.blindtest.model.Game;

public interface GameRepository extends JpaRepository<Game, Integer> {

    Optional<Game> findByFranchiseAndName(Franchise franchise, String name);

    // join fetch charge la franchise dans la même requête (sinon Lazy) : tous les jeux, pour la recherche à la réponse d'un blindtest
    @Query("""
            SELECT g FROM Game g
            JOIN FETCH g.franchise f
            ORDER BY g.name
            """)
    List<Game> findAllWithFranchise();
}
