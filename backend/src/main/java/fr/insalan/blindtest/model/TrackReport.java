package fr.insalan.blindtest.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;

@Entity
public class TrackReport {

    @EmbeddedId
    private TrackReportId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("trackId")
    @JoinColumn(name = "track_id")
    private Track track;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("listenerId")
    @JoinColumn(name = "listener_id")
    private Listener listener;

    @Column(nullable = false, columnDefinition = "datetime")
    private Instant createdAt;

    protected TrackReport() {
        // JPA
    }

    public TrackReport(Track track, Listener listener) {
        this.id = new TrackReportId(track.getId(), listener.getId());
        this.track = track;
        this.listener = listener;
        this.createdAt = Instant.now();
    }

    public Track getTrack() {
        return track;
    }

    public Listener getListener() {
        return listener;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
