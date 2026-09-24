package fr.insalan.blindtest.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.insalan.blindtest.model.Tag;

public interface TagRepository extends JpaRepository<Tag, Integer> {

}
