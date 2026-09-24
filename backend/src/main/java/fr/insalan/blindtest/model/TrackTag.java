package fr.insalan.blindtest.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;

@Entity
public class TrackTag {

    @EmbeddedId
    private TrackTagId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("trackId")
    @JoinColumn(name = "track_id")
    private Track track;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("tagId")
    @JoinColumn(name = "tag_id")
    private Tag tag;

    protected TrackTag() {
        // JPA
    }

    public TrackTag(Track track, Tag tag) {
        this.id = new TrackTagId(track.getId(), tag.getId());
        this.track = track;
        this.tag = tag;
    }

    public Track getTrack() {
        return track;
    }

    public Tag getTag() {
        return tag;
    }
}
