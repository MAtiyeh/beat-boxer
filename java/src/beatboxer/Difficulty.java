package beatboxer;

public enum Difficulty {
    EASY("Easy", 1500, 700),
    MEDIUM("Medium", 1000, 500),
    HARD("Hard", 650, 350);

    public final String label;
    public final long windowMs;  // time allowed to hit the target
    public final long gapMs;     // pause between targets

    Difficulty(String label, long windowMs, long gapMs) {
        this.label = label;
        this.windowMs = windowMs;
        this.gapMs = gapMs;
    }
}
