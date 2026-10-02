package fr.insalan.blindtest.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // même chose que findAllWithFranchise, mais pour un seul jeu
    @Query("""
            SELECT g FROM Game g
            JOIN FETCH g.franchise f
            WHERE g.id = :id
            """)
    Optional<Game> findByIdWithFranchise(@Param("id") Integer id);

    boolean existsByFranchiseAndNameIgnoreCase(Franchise franchise, String name);

    boolean existsByFranchiseAndNameIgnoreCaseAndIdNot(Franchise franchise, String name, Integer id);

    @Query(value = """
            SELECT s.name FROM game s
            JOIN game t ON lower(s.name) = lower(t.name)
            WHERE s.franchise_id = :sourceId AND t.franchise_id = :targetId
            ORDER BY s.name
            """, nativeQuery = true)
    List<String> findNamesInBothFranchises(@Param("sourceId") Integer sourceId, @Param("targetId") Integer targetId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE game SET franchise_id = :targetId WHERE franchise_id = :sourceId", nativeQuery = true)
    void moveGamesToFranchise(@Param("sourceId") Integer sourceId, @Param("targetId") Integer targetId);
}
