package fr.insalan.blindtest.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;

@Entity
public class BlindtestDifficultyBand {

    @EmbeddedId
    private BlindtestDifficultyBandId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("blindtestId")
    @JoinColumn(name = "blindtest_id")
    private Blindtest blindtest;

    @Column(nullable = false)
    private int minDifficulty;

    @Column(nullable = false)
    private int maxDifficulty;

    @Column(nullable = false)
    private int proportion;

    protected BlindtestDifficultyBand() {
        // JPA
    }

    public BlindtestDifficultyBand(Blindtest blindtest, int position, int minDifficulty, int maxDifficulty, int proportion) {
        this.id = new BlindtestDifficultyBandId(blindtest.getId(), position);
        this.blindtest = blindtest;
        this.minDifficulty = minDifficulty;
        this.maxDifficulty = maxDifficulty;
        this.proportion = proportion;
    }

    public Blindtest getBlindtest() {
        return blindtest;
    }

    public int getPosition() {
        return id.position();
    }

    public int getMinDifficulty() {
        return minDifficulty;
    }

    public int getMaxDifficulty() {
        return maxDifficulty;
    }

    public int getProportion() {
        return proportion;
    }
}
