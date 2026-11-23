package beatboxer;

/** Decides how many points a hit is worth and keeps track of the combo. */
public class ScoreManager {
    private int score, combo, bestCombo, hits, misses, wrongs;
    private long reactionSum;

    public void reset() { score = combo = bestCombo = hits = misses = wrongs = 0; reactionSum = 0; }

    /** @return points awarded */
    public int registerHit(long reactionMs, long windowMs) {
        hits++;
        combo++;
        bestCombo = Math.max(bestCombo, combo);
        reactionSum += reactionMs;
        double speed = Math.max(0, 1.0 - (double) reactionMs / windowMs);   // 1 = instant
        int base = 100 + (int) Math.round(100 * speed);
        double mult = 1.0 + Math.min(combo - 1, 10) * 0.1;                  // up to x2
        int pts = (int) Math.round(base * mult);
        score += pts;
        return pts;
    }

    public void registerWrong() { wrongs++; combo = 0; }
    public void registerMiss()  { misses++; combo = 0; }

    public int getScore() { return score; }
    public int getCombo() { return combo; }
    public int getBestCombo() { return bestCombo; }
    public int getHits() { return hits; }
    public int getMisses() { return misses; }
    public int getWrongs() { return wrongs; }
    public int getAttempts() { return hits + misses + wrongs; }
    public int getAccuracy() { return getAttempts() == 0 ? 0 : (int) Math.round(100.0 * hits / getAttempts()); }
    public long getAvgReaction() { return hits == 0 ? 0 : reactionSum / hits; }
}
