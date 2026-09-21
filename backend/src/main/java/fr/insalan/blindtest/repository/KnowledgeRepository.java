package fr.insalan.blindtest.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.insalan.blindtest.model.Knowledge;
import fr.insalan.blindtest.model.KnowledgeId;

public interface KnowledgeRepository extends JpaRepository<Knowledge, KnowledgeId> {
    
}
