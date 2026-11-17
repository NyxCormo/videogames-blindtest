package fr.insalan.blindtest.room;

class RoomScore {

    static final int FRANCHISE_POINTS = 250;
    static final int BONUS_POINTS = 250;

    private int score;
    private int roundPoints;
    private int attemptsUsed;
    private boolean franchiseFound;
    private boolean gameFound;
    private boolean bonusFound;
    private boolean passed;
    private int gamesFound;
    private int franchisesFound;
    private int bonusesFound;
    private int tracksHeard;

    void findGame(int points, boolean bonus) {
        gameFound = true;
        franchiseFound = true;
        bonusFound = bonus;
        roundPoints = bonus ? points + BONUS_POINTS : points;
    }

    void missGame() {
        attemptsUsed++;
    }

    void guessFranchise(boolean correct) {
        if (correct && !franchiseFound) {
            franchiseFound = true;
            roundPoints = FRANCHISE_POINTS;
        }
        attemptsUsed++;
    }

    void pass() {
        passed = true;
    }

    boolean done(int maxAttempts) {
        return gameFound || passed || attemptsUsed >= maxAttempts;
    }

    void endRound(boolean present) {
        score += roundPoints;
        if (present || answered()) {
            tracksHeard++;
        }
        if (gameFound) {
            gamesFound++;
        } else if (franchiseFound) {
            franchisesFound++;
        }
        if (bonusFound) {
            bonusesFound++;
        }
    }

    private boolean answered() {
        return attemptsUsed > 0 || franchiseFound || gameFound || passed;
    }

    RoomTotals totals() {
        return new RoomTotals(gamesFound, franchisesFound, bonusesFound, tracksHeard);
    }

    void newRound() {
        roundPoints = 0;
        attemptsUsed = 0;
        franchiseFound = false;
        gameFound = false;
        bonusFound = false;
        passed = false;
    }

    RoomRound round(int position, Integer trackId, Integer franchiseId, int maxAttempts) {
        return new RoomRound(
            position,
            trackId,
            score,
            roundPoints,
            Math.max(0, maxAttempts - attemptsUsed),
            franchiseFound ? franchiseId : null,
            gameFound,
            bonusFound,
            done(maxAttempts)
        );
    }
}
