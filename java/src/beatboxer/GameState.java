package beatboxer;

/** State pattern: the game behaves differently in Ready, Playing and Results. */
public interface GameState {
    void enter(BeatBoxerGame game);
    void tick(long nowMs);
    void onPadHit(char side, long timeMs);
    void onKey(int keyCode);
    String name();
}
