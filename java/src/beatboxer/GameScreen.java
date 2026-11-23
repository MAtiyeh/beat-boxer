package beatboxer;

import javax.swing.*;
import java.awt.*;

/** Draws the active target, timer, score, combo and the end-of-round result. */
public class GameScreen extends JPanel {
    private static final Color BG = new Color(18, 20, 28);
    private static final Color PANEL = new Color(34, 38, 52);
    private static final Color TEXT = new Color(235, 238, 245);
    private static final Color MUTED = new Color(140, 148, 170);
    private static final Color LEFT_C = new Color(70, 130, 255);
    private static final Color RIGHT_C = new Color(255, 200, 40);
    private static final Color GOOD = new Color(60, 210, 120);
    private static final Color BAD = new Color(240, 80, 80);

    private final BeatBoxerGame game;

    public GameScreen(BeatBoxerGame game) {
        this.game = game;
        setBackground(BG);
        setPreferredSize(new Dimension(1100, 700));
        setFocusable(true);
    }

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        GameState s = game.getState();

        drawStatusBar(g, w, h);
        if (s instanceof ReadyState) drawReady(g, w, h);
        else if (s instanceof PlayingState) drawPlaying(g, w, h, (PlayingState) s);
        else if (s instanceof ResultsState) drawResults(g, w, h, (ResultsState) s);
    }

    private void center(Graphics2D g, String text, int y, Font f, Color c, int w) {
        g.setFont(f);
        g.setColor(c);
        int tw = g.getFontMetrics().stringWidth(text);
        g.drawString(text, (w - tw) / 2, y);
    }

    private void drawStatusBar(Graphics2D g, int w, int h) {
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(MUTED);
        String links = "XBee: " + game.getXbeeStatus() + "    Pad USB: " + game.getPadStatus()
                + "    Glove: " + (game.isGloveSeen() ? "connected" : "not seen")
                + "    Last msg: " + game.getLastMessage();
        g.drawString(links, 16, h - 14);
    }

    private void drawReady(Graphics2D g, int w, int h) {
        center(g, "BEAT BOXER", h / 4, new Font("SansSerif", Font.BOLD, 72), TEXT, w);
        center(g, "Punch the pad that lights up before time runs out", h / 4 + 50, new Font("SansSerif", Font.PLAIN, 22), MUTED, w);

        int y = h / 2 - 10;
        Difficulty[] ds = Difficulty.values();
        int bw = 200, gap = 30, total = ds.length * bw + (ds.length - 1) * gap, x = (w - total) / 2;
        for (int i = 0; i < ds.length; i++) {
            boolean sel = ds[i] == game.getDifficulty();
            g.setColor(sel ? LEFT_C : PANEL);
            g.fillRoundRect(x, y, bw, 70, 18, 18);
            g.setColor(TEXT);
            g.setFont(new Font("SansSerif", Font.BOLD, 24));
            String t = (i + 1) + "  " + ds[i].label;
            g.drawString(t, x + (bw - g.getFontMetrics().stringWidth(t)) / 2, y + 44);
            x += bw + gap;
        }
        center(g, "High score (" + game.getDifficulty().label + "): " + game.getHighScore(), y + 120, new Font("SansSerif", Font.PLAIN, 22), TEXT, w);
        center(g, "Require glove punch: " + (game.isRequireGlove() ? "ON" : "OFF") + "   (press G)", y + 155, new Font("SansSerif", Font.PLAIN, 18), MUTED, w);
        center(g, "Press 1 / 2 / 3 to pick difficulty  -  punch any pad or press SPACE to start", y + 210, new Font("SansSerif", Font.BOLD, 20), GOOD, w);
        center(g, "Keyboard test: A = left pad, L = right pad", y + 240, new Font("SansSerif", Font.PLAIN, 16), MUTED, w);
    }

    private void drawPlaying(Graphics2D g, int w, int h, PlayingState p) {
        ScoreManager sc = game.getScore();
        long now = System.currentTimeMillis();

        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(TEXT);
        g.drawString("Score " + sc.getScore(), 30, 50);
        String combo = "Combo x" + sc.getCombo();
        g.drawString(combo, w - 30 - g.getFontMetrics().stringWidth(combo), 50);
        center(g, "Beat " + Math.min(p.getBeat(), PlayingState.BEATS_PER_ROUND) + " / " + PlayingState.BEATS_PER_ROUND + "   -   " + game.getDifficulty().label,
                50, new Font("SansSerif", Font.PLAIN, 20), MUTED, w);

        if (p.getPhase() == PlayingState.Phase.COUNTDOWN) {
            center(g, p.getCountdown() > 0 ? String.valueOf(p.getCountdown()) : "GO!", h / 2 + 60, new Font("SansSerif", Font.BOLD, 200), TEXT, w);
            return;
        }

        int padW = (int) (w * 0.32), padH = (int) (h * 0.45), top = h / 2 - padH / 2 + 10;
        int lx = w / 2 - padW - 40, rx = w / 2 + 40;
        boolean cue = p.getPhase() == PlayingState.Phase.CUE;
        drawPad(g, lx, top, padW, padH, "LEFT", LEFT_C, cue && p.getTarget() == 'L');
        drawPad(g, rx, top, padW, padH, "RIGHT", RIGHT_C, cue && p.getTarget() == 'R');

        if (cue) {
            double f = p.getTimeLeftFraction(now);
            int bw = w - 160;
            g.setColor(PANEL);
            g.fillRoundRect(80, top + padH + 30, bw, 18, 9, 9);
            g.setColor(f > 0.35 ? GOOD : BAD);
            g.fillRoundRect(80, top + padH + 30, (int) (bw * f), 18, 9, 9);
        } else {
            Color c = p.isLastWasGood() ? GOOD : BAD;
            center(g, p.getMessage(), top + padH + 70, new Font("SansSerif", Font.BOLD, 56), c, w);
            if (p.getLastPoints() > 0)
                center(g, "+" + p.getLastPoints(), top + padH + 110, new Font("SansSerif", Font.BOLD, 30), c, w);
        }
    }

    private void drawPad(Graphics2D g, int x, int y, int pw, int ph, String label, Color c, boolean active) {
        g.setColor(active ? c : PANEL);
        g.fillRoundRect(x, y, pw, ph, 30, 30);
        g.setStroke(new BasicStroke(active ? 6 : 2));
        g.setColor(active ? Color.WHITE : c.darker());
        g.drawRoundRect(x, y, pw, ph, 30, 30);
        g.setFont(new Font("SansSerif", Font.BOLD, active ? 64 : 40));
        g.setColor(active ? BG : MUTED);
        String t = active ? "HIT " + label + "!" : label;
        int tw = g.getFontMetrics().stringWidth(t);
        g.drawString(t, x + (pw - tw) / 2, y + ph / 2 + 20);
    }

    private void drawResults(Graphics2D g, int w, int h, ResultsState r) {
        ScoreManager sc = game.getScore();
        center(g, "ROUND OVER", h / 5, new Font("SansSerif", Font.BOLD, 64), TEXT, w);
        if (r.isNewHigh()) center(g, "NEW HIGH SCORE!", h / 5 + 55, new Font("SansSerif", Font.BOLD, 36), GOOD, w);
        String[] lines = {
                "Score: " + sc.getScore(),
                "Best combo: " + sc.getBestCombo(),
                "Accuracy: " + sc.getAccuracy() + "%   (" + sc.getHits() + " hits, " + sc.getWrongs() + " wrong, " + sc.getMisses() + " missed)",
                "Average reaction: " + sc.getAvgReaction() + " ms",
                "High score (" + game.getDifficulty().label + "): " + game.getHighScore()
        };
        int y = h / 5 + 130;
        for (String l : lines) { center(g, l, y, new Font("SansSerif", Font.PLAIN, 30), TEXT, w); y += 50; }
        center(g, "Punch a pad or press SPACE to play again", y + 30, new Font("SansSerif", Font.BOLD, 22), GOOD, w);
    }
}
