package fr.insalan.blindtest.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fr.insalan.blindtest.model.Tag;
import fr.insalan.blindtest.model.TagType;

public interface TagRepository extends JpaRepository<Tag, Integer> {

    Optional<Tag> findByTypeAndName(TagType type, String name);
    
    @Query("""
            SELECT t FROM Tag t
            JOIN FETCH t.type
            WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%'))
            ORDER BY t.name
            """)
    List<Tag> searchWithType(@Param("search") String search, Pageable limit);

    // tous les tags, avec leur type (pour la liste complète filtrable côté front)
    @Query("""
            SELECT t FROM Tag t
            JOIN FETCH t.type
            ORDER BY t.name
            """)
    List<Tag> findAllWithType();

    @Query("""
            SELECT t FROM Tag t
            JOIN FETCH t.type
            WHERE t.id IN :ids
            """)
    List<Tag> findAllByIdInWithType(@Param("ids") List<Integer> ids);
}
