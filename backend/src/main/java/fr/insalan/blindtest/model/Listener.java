package fr.insalan.blindtest.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity 
public class Listener {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "datetime")
    private Instant createdAt;

    @Column(columnDefinition = "datetime")
    private Instant lastSeenAt;

    protected Listener() {
        // JPA
    }

    public Listener(String name) {
        this.name = name;
        this.createdAt = Instant.now();
        this.lastSeenAt = this.createdAt;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void markSeen() {
        this.lastSeenAt = Instant.now();
    }
}
