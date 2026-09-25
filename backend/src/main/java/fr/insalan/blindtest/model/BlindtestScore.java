package fr.insalan.blindtest.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;

@Entity
public class BlindtestScore {

    @EmbeddedId
    private BlindtestScoreId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("blindtestId")
    @JoinColumn(name = "blindtest_id")
    private Blindtest blindtest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("listenerId")
    @JoinColumn(name = "listener_id")
    private Listener listener;

    @Column(nullable = false)
    private int goodAnswers;

    @Column(nullable = false)
    private int franchiseAnswers;

    @Column(nullable = false)
    private int bonusAnswers;

    @Column(nullable = false)
    private int tracksHeard;

    @Column(nullable = false)
    private int totalAttempts;

    @Column(nullable = false)
    private int attemptsUsedOnCurrentTrack;

    protected BlindtestScore() {
        // JPA
    }

    public BlindtestScore(Blindtest blindtest, Listener listener) {
        this.id = new BlindtestScoreId(blindtest.getId(), listener.getId());
        this.blindtest = blindtest;
        this.listener = listener;
        this.goodAnswers = 0;
        this.franchiseAnswers = 0;
        this.bonusAnswers = 0;
        this.tracksHeard = 0;
        this.totalAttempts = 0;
        this.attemptsUsedOnCurrentTrack = 0;
    }

    public Blindtest getBlindtest() {
        return blindtest;
    }

    public Listener getListener() {
        return listener;
    }

    public int getGoodAnswers() {
        return goodAnswers;
    }

    public void setGoodAnswers(int goodAnswers) {
        this.goodAnswers = goodAnswers;
    }

    public int getFranchiseAnswers() {
        return franchiseAnswers;
    }

    public void setFranchiseAnswers(int franchiseAnswers) {
        this.franchiseAnswers = franchiseAnswers;
    }

    public int getBonusAnswers() {
        return bonusAnswers;
    }

    public void setBonusAnswers(int bonusAnswers) {
        this.bonusAnswers = bonusAnswers;
    }

    public int getTracksHeard() {
        return tracksHeard;
    }

    public void setTracksHeard(int tracksHeard) {
        this.tracksHeard = tracksHeard;
    }

    public int getTotalAttempts() {
        return totalAttempts;
    }

    public void setTotalAttempts(int totalAttempts) {
        this.totalAttempts = totalAttempts;
    }

    public int getAttemptsUsedOnCurrentTrack() {
        return attemptsUsedOnCurrentTrack;
    }

    public void setAttemptsUsedOnCurrentTrack(int attemptsUsedOnCurrentTrack) {
        this.attemptsUsedOnCurrentTrack = attemptsUsedOnCurrentTrack;
    }
}
