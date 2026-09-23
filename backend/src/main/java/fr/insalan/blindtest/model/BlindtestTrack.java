package fr.insalan.blindtest.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;

@Entity
public class BlindtestTrack {

    @EmbeddedId
    private BlindtestTrackId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("blindtestId")
    @JoinColumn(name = "blindtest_id")
    private Blindtest blindtest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "track_id")
    private Track track;

    protected BlindtestTrack() {
        // JPA
    }

    public BlindtestTrack(Blindtest blindtest, Track track, int position) {
        this.id = new BlindtestTrackId(blindtest.getId(), position);
        this.blindtest = blindtest;
        this.track = track;
    }

    public Blindtest getBlindtest() {
        return blindtest;
    }

    public Track getTrack() {
        return track;
    }

    public int getPosition() {
        return id.position();
    }
}
