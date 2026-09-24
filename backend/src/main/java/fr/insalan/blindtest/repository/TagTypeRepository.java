package fr.insalan.blindtest.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.insalan.blindtest.model.TagType;

public interface TagTypeRepository extends JpaRepository<TagType, Integer> {

    Optional<TagType> findByName(String name);
}
