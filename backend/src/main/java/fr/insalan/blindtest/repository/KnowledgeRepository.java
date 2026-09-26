package fr.insalan.blindtest.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fr.insalan.blindtest.model.Knowledge;
import fr.insalan.blindtest.model.KnowledgeId;

public interface KnowledgeRepository extends JpaRepository<Knowledge, KnowledgeId> {

    @Query("SELECT k FROM Knowledge k WHERE k.listener.id = :listenerId")
    List<Knowledge> findByListenerId(@Param("listenerId") Integer listenerId);
}
