package beatboxer;

import java.awt.event.KeyEvent;
import java.util.Random;

public class PlayingState implements GameState {
    public enum Phase { COUNTDOWN, CUE, RESULT }

    public static final int BEATS_PER_ROUND = 20;
    private static final long COUNT_STEP_MS = 700;
    private static final long GLOVE_WINDOW_MS = 600;

    private BeatBoxerGame game;
    private final Random random = new Random();

    private Phase phase = Phase.COUNTDOWN;
    private long phaseStart;
    private int countdown = 3;
    private char target = 'L';
    private int beat = 0;
    private String message = "";
    private int lastPoints = 0;
    private boolean lastWasGood = false;

    @Override public void enter(BeatBoxerGame game) {
        this.game = game;
        game.getScore().reset();
        phase = Phase.COUNTDOWN;
        countdown = 3;
        phaseStart = System.currentTimeMillis();
        game.getFeedback().off();
        game.getFeedback().countdown();
    }

    @Override public void tick(long now) {
        Difficulty d = game.getDifficulty();
        switch (phase) {
            case COUNTDOWN:
                if (now - phaseStart >= COUNT_STEP_MS) {
                    countdown--;
                    phaseStart = now;
                    if (countdown > 0) game.getFeedback().countdown();
                    else { game.getFeedback().go(); startCue(now); }
                }
                break;
            case CUE:
                if (now - phaseStart > d.windowMs) {          // too late
                    game.getScore().registerMiss();
                    game.getFeedback().wrong();
                    showResult(now, "TOO LATE", 0, false);
                }
                break;
            case RESULT:
                if (now - phaseStart > d.gapMs) {
                    game.getFeedback().off();
                    if (beat >= BEATS_PER_ROUND) game.setState(new ResultsState());
                    else startCue(now);
                }
                break;
        }
    }

    private void startCue(long now) {
        beat++;
        target = random.nextBoolean() ? 'L' : 'R';
        phase = Phase.CUE;
        phaseStart = now;
        message = "";
        game.getFeedback().showTarget(target);
    }

    private void showResult(long now, String msg, int pts, boolean good) {
        phase = Phase.RESULT;
        phaseStart = now;
        message = msg;
        lastPoints = pts;
        lastWasGood = good;
    }

    @Override public void onPadHit(char side, long timeMs) {
        if (phase != Phase.CUE) return;
        long now = System.currentTimeMillis();
        long reaction = Math.max(0, timeMs - phaseStart);
        if (side != target) {
            game.getScore().registerWrong();
            game.getFeedback().wrong();
            showResult(now, "WRONG SIDE", 0, false);
            return;
        }
        if (game.isRequireGlove() && Math.abs(timeMs - game.getLastGlovePunch()) > GLOVE_WINDOW_MS) {
            game.getScore().registerWrong();
            game.getFeedback().wrong();
            showResult(now, "NO PUNCH DETECTED", 0, false);
            return;
        }
        int pts = game.getScore().registerHit(reaction, game.getDifficulty().windowMs);
        game.getFeedback().correct();
        String word = reaction < game.getDifficulty().windowMs * 0.35 ? "PERFECT!" : reaction < game.getDifficulty().windowMs * 0.7 ? "GREAT!" : "GOOD";
        showResult(now, word, pts, true);
    }

    @Override public void onKey(int k) {
        if (k == KeyEvent.VK_ESCAPE) game.setState(new ReadyState());
    }

    @Override public String name() { return "PLAYING"; }

    public Phase getPhase() { return phase; }
    public int getCountdown() { return countdown; }
    public char getTarget() { return target; }
    public int getBeat() { return beat; }
    public String getMessage() { return message; }
    public int getLastPoints() { return lastPoints; }
    public boolean isLastWasGood() { return lastWasGood; }
    public double getTimeLeftFraction(long now) {
        if (phase != Phase.CUE) return 0;
        return Math.max(0, 1.0 - (double) (now - phaseStart) / game.getDifficulty().windowMs);
    }
}
