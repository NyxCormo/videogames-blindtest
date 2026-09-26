package fr.insalan.blindtest.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Blindtest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int difficulty;

    @Column(nullable = false)
    private int maxAttempts;

    @Column(nullable = false, columnDefinition = "datetime")
    private Instant createdAt;

    protected Blindtest() {
        // JPA
    }

    public Blindtest(String name, int difficulty, int maxAttempts) {
        this.name = name;
        this.difficulty = difficulty;
        this.maxAttempts = maxAttempts;
        this.createdAt = Instant.now();
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
