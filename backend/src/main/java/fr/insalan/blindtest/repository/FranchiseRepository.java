package fr.insalan.blindtest.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.insalan.blindtest.model.Franchise;

public interface FranchiseRepository extends JpaRepository<Franchise, Integer> {
}
