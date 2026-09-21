package fr.insalan.blindtest.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;

@Entity 
public class Knowledge {
    
    @EmbeddedId 
    private KnowledgeId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("listenerId")
    @JoinColumn(name = "listener_id")
    private Listener listener;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("trackId")
    @JoinColumn(name = "track_id")
    private Track track;

    @Column(nullable = false, columnDefinition = "integer")
    private boolean knows;

    protected Knowledge(){
        // JPA
    }

    public Knowledge(Listener listener, Track track, Boolean knows) {
        this.id = new KnowledgeId(listener.getId(), track.getId());
        this.listener = listener;
        this.track = track;
        this.knows = knows;
    }

    public Listener getListener() {
        return listener;
    }

    public Track getTrack() {
        return track;
    }

    public boolean isKnows() {
        return knows;
    }
}
