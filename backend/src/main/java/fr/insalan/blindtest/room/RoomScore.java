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

    void endRound() {
        score += roundPoints;
    }

    void newRound() {
        roundPoints = 0;
        attemptsUsed = 0;
        franchiseFound = false;
        gameFound = false;
        bonusFound = false;
        passed = false;
    }

    RoomRound round(int position, Integer trackId, int maxAttempts) {
        return new RoomRound(
            position,
            trackId,
            score,
            roundPoints,
            Math.max(0, maxAttempts - attemptsUsed),
            franchiseFound,
            gameFound,
            bonusFound,
            done(maxAttempts)
        );
    }
}
