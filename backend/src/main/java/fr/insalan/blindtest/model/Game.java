package fr.insalan.blindtest.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Game {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(nullable = false)
	private String name;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "franchise_id")
	private Franchise franchise;

	protected Game() {
        // Default constructor for JPA
	}

	public Game(String name, Franchise franchise) {
		this.name = name;
		this.franchise = franchise;
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Franchise getFranchise() {
		return franchise;
	}

}
