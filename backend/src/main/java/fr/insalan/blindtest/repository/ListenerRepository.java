package fr.insalan.blindtest.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import fr.insalan.blindtest.model.Listener;

public interface ListenerRepository extends JpaRepository<Listener, Integer> {
    
    Optional<Listener> findByName(String name);

    boolean existsByNameIgnoreCase(String name);

    List<Listener> findTop10ByNameContainingIgnoreCaseOrderByName(String search);

    List<Listener> findAllByOrderByNameAsc();

    @Query(value = """
            SELECT l.id AS id, l.name AS name,
                   (SELECT count(*) FROM knowledge k WHERE k.listener_id = l.id) AS votes,
                   (SELECT count(*) FROM blindtest_score s WHERE s.listener_id = l.id) AS blindtests
            FROM listener l
            ORDER BY l.name
            """, nativeQuery = true)
    List<ListenerUsage> findAllWithUsage();
}
