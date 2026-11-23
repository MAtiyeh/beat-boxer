package beatboxer;

import java.awt.event.KeyEvent;

public class ResultsState implements GameState {
    private BeatBoxerGame game;
    private boolean newHigh;
    private long enteredAt;

    @Override public void enter(BeatBoxerGame game) {
        this.game = game;
        enteredAt = System.currentTimeMillis();
        newHigh = game.submitHighScore(game.getScore().getScore());
        game.getFeedback().off();
        if (newHigh) game.getFeedback().correct();
    }
    @Override public void tick(long nowMs) { }
    @Override public void onPadHit(char side, long timeMs) {
        if (System.currentTimeMillis() - enteredAt > 2000) game.setState(new ReadyState());
    }
    @Override public void onKey(int k) {
        if (k == KeyEvent.VK_SPACE || k == KeyEvent.VK_ENTER || k == KeyEvent.VK_ESCAPE) game.setState(new ReadyState());
    }
    @Override public String name() { return "RESULTS"; }
    public boolean isNewHigh() { return newHigh; }
}
