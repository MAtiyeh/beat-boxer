package beatboxer;

import com.fazecast.jSerialComm.SerialPort;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.Properties;

/**
 * Main controller: starts and ends rounds, receives sensor events (Observer)
 * and passes them to the current GameState (State pattern).
 */
public class BeatBoxerGame implements SensorListener {
    private final ScoreManager score = new ScoreManager();
    private FeedbackController feedback = new FeedbackController(null, null);
    private ZigBeeManager xbee, pad;
    private GameState state;
    private GameScreen screen;
    private Difficulty difficulty = Difficulty.MEDIUM;
    private boolean requireGlove = false;
    private volatile long lastGlovePunch = -100000;
    private volatile boolean gloveSeen = false;
    private volatile String lastMessage = "-";
    private final Properties highScores = new Properties();
    private final File hsFile = new File(System.getProperty("user.home"), "beatboxer_highscores.properties");

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BeatBoxerGame().start());
    }

    private void start() {
        loadHighScores();
        chooseAndOpenPorts();
        feedback = new FeedbackController(pad, xbee);

        JFrame frame = new JFrame("Beat Boxer");
        screen = new GameScreen(this);
        frame.setContentPane(screen);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) {
                feedback.off();
                if (xbee != null) xbee.close();
                if (pad != null) pad.close();
            }
        });
        screen.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_A) state.onPadHit('L', System.currentTimeMillis());
                else if (e.getKeyCode() == KeyEvent.VK_L) state.onPadHit('R', System.currentTimeMillis());
                else state.onKey(e.getKeyCode());
                screen.repaint();
            }
        });
        setState(new ReadyState());
        frame.setVisible(true);
        screen.requestFocusInWindow();

        new Timer(16, e -> { state.tick(System.currentTimeMillis()); screen.repaint(); }).start();
    }

    private void chooseAndOpenPorts() {
        SerialPort[] ports = SerialPort.getCommPorts();
        String[] names = new String[ports.length + 1];
        names[0] = "(none)";
        for (int i = 0; i < ports.length; i++)
            names[i + 1] = ports[i].getSystemPortName() + " - " + ports[i].getPortDescription();

        JComboBox<String> xbeeBox = new JComboBox<>(names);
        JComboBox<String> padBox = new JComboBox<>(names);
        xbeeBox.setSelectedIndex(guess(names, "COM9"));
        padBox.setSelectedIndex(guess(names, "COM12"));

        JPanel p = new JPanel(new GridLayout(0, 1, 6, 6));
        p.add(new JLabel("XBee coordinator (red XBee Explorer board):"));
        p.add(xbeeBox);
        p.add(new JLabel("Pad node USB cable (FTDI board on the pad LilyPad) - for LED and buzzer:"));
        p.add(padBox);
        p.add(new JLabel("Tip: unplug a cable and press Refresh to see which COM disappears."));

        Object[] options = {"Start", "Refresh", "Keyboard only"};
        int r = JOptionPane.showOptionDialog(null, p, "Beat Boxer - connect", JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (r == 1) { chooseAndOpenPorts(); return; }
        if (r != 0) return;

        String xName = portName(names[xbeeBox.getSelectedIndex()]);
        String pName = portName(names[padBox.getSelectedIndex()]);
        if (xName != null) {
            xbee = new ZigBeeManager(xName);
            if (xbee.open()) xbee.addListener(this);
            else { warn("Could not open " + xName + " (close XCTU / Arduino Serial Monitor first)"); xbee = null; }
        }
        if (pName != null && !pName.equals(xName)) {
            pad = new ZigBeeManager(pName);
            if (pad.open()) pad.addListener(this);
            else { warn("Could not open " + pName + " (close Arduino Serial Monitor first)"); pad = null; }
        }
    }

    private static int guess(String[] names, String com) {
        for (int i = 1; i < names.length; i++) if (names[i].startsWith(com + " ")) return i;
        return 0;
    }

    private static String portName(String entry) {
        return entry.startsWith("(") ? null : entry.split(" ")[0];
    }

    private void warn(String msg) { JOptionPane.showMessageDialog(null, msg, "Beat Boxer", JOptionPane.WARNING_MESSAGE); }

    /** Observer callback - runs on the serial thread, so hand it to the Swing thread. */
    @Override public void onPacket(SensorPacket pkt) {
        lastMessage = pkt.toString();
        if (pkt.node.equals("G")) {
            gloveSeen = true;
            if (pkt.type.equals("PUNCH")) lastGlovePunch = pkt.timeMs;
            return;
        }
        if (pkt.type.equals("L") || pkt.type.equals("R")) {
            // Pad hits arrive both by XBee and by USB. Use XBee when it is connected; USB otherwise.
            boolean fromXbee = xbee != null && pkt.source.equals(xbee.getPortName());
            if (xbee != null && !fromXbee) return;
            char side = pkt.type.charAt(0);
            long t = pkt.timeMs;
            SwingUtilities.invokeLater(() -> state.onPadHit(side, t));
        }
    }

    public void setState(GameState s) {
        state = s;
        s.enter(this);
        if (screen != null) screen.repaint();
    }

    // ---- high scores ----
    private void loadHighScores() {
        try (InputStream in = new FileInputStream(hsFile)) { highScores.load(in); } catch (IOException ignored) { }
    }
    public int getHighScore() {
        try { return Integer.parseInt(highScores.getProperty(difficulty.name(), "0")); } catch (Exception e) { return 0; }
    }
    public boolean submitHighScore(int s) {
        if (s <= getHighScore()) return false;
        highScores.setProperty(difficulty.name(), String.valueOf(s));
        try (OutputStream out = new FileOutputStream(hsFile)) { highScores.store(out, "Beat Boxer high scores"); } catch (IOException ignored) { }
        return true;
    }

    // ---- getters / setters ----
    public GameState getState() { return state; }
    public ScoreManager getScore() { return score; }
    public FeedbackController getFeedback() { return feedback; }
    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty d) { difficulty = d; }
    public boolean isRequireGlove() { return requireGlove; }
    public void setRequireGlove(boolean b) { requireGlove = b; }
    public long getLastGlovePunch() { return lastGlovePunch; }
    public boolean isGloveSeen() { return gloveSeen; }
    public String getLastMessage() { return lastMessage; }
    public String getXbeeStatus() { return xbee == null ? "off" : xbee.getPortName(); }
    public String getPadStatus() { return pad == null ? "off" : pad.getPortName(); }
}
