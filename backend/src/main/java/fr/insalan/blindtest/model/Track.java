package fr.insalan.blindtest.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Track {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(nullable = false)
	private String name;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "game_id")
	private Game game;

	private String khinsiderLink;

	private String youtubeLink;

	private String audioLink;

	// SQLite n'a pas de type "datetime", on utilise donc un Instant (Integer en milliseconds), qui sera converti en timestamp par Hibernate.
	@Column(columnDefinition = "datetime")
	private Instant audioLinkResolvedAt;

	private Integer duration;

	private Integer preferredStart;

	protected Track() {
		// Default constructor for JPA
	}

	public Track(String name, Game game) {
		this.name = name;
		this.game = game;
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Game getGame() {
		return game;
	}

	public String getKhinsiderLink() {
		return khinsiderLink;
	}

	public void setKhinsiderLink(String khinsiderLink) {
		this.khinsiderLink = khinsiderLink;
	}

	public String getYoutubeLink() {
		return youtubeLink;
	}

	public void setYoutubeLink(String youtubeLink) {
		this.youtubeLink = youtubeLink;
	}

	public String getAudioLink() {
		return audioLink;
	}

	public void setAudioLink(String audioLink) {
		this.audioLink = audioLink;
	}

	public Instant getAudioLinkResolvedAt() {
		return audioLinkResolvedAt;
	}

	public void setAudioLinkResolvedAt(Instant audioLinkResolvedAt) {
		this.audioLinkResolvedAt = audioLinkResolvedAt;
	}

	public Integer getDuration() {
		return duration;
	}

	public void setDuration(Integer duration) {
		this.duration = duration;
	}

	public Integer getPreferredStart() {
		return preferredStart;
	}

	public void setPreferredStart(Integer preferredStart) {
		this.preferredStart = preferredStart;
	}

}
