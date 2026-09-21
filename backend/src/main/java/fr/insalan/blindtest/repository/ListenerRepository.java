package fr.insalan.blindtest.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.insalan.blindtest.model.Listener;

public interface ListenerRepository extends JpaRepository<Listener, Integer> {
    
    Optional<Listener> findByName(String name);
}
