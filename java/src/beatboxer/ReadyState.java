package beatboxer;

import java.awt.event.KeyEvent;

public class ReadyState implements GameState {
    private BeatBoxerGame game;

    @Override public void enter(BeatBoxerGame game) { this.game = game; game.getFeedback().off(); }
    @Override public void tick(long nowMs) { }
    @Override public void onPadHit(char side, long timeMs) {
        // punch any pad to start
        game.setState(new PlayingState());
    }
    @Override public void onKey(int k) {
        if (k == KeyEvent.VK_1) game.setDifficulty(Difficulty.EASY);
        else if (k == KeyEvent.VK_2) game.setDifficulty(Difficulty.MEDIUM);
        else if (k == KeyEvent.VK_3) game.setDifficulty(Difficulty.HARD);
        else if (k == KeyEvent.VK_G) game.setRequireGlove(!game.isRequireGlove());
        else if (k == KeyEvent.VK_SPACE || k == KeyEvent.VK_ENTER) game.setState(new PlayingState());
    }
    @Override public String name() { return "READY"; }
}
