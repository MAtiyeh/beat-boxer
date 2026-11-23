package beatboxer;

/**
 * Sends light / sound / vibration commands to the hardware.
 * Commands go to the pad node's USB cable (reliable) and are also
 * broadcast through the XBee coordinator.
 */
public class FeedbackController {
    private final ZigBeeManager padLink;   // may be null
    private final ZigBeeManager xbeeLink;  // may be null

    public FeedbackController(ZigBeeManager padLink, ZigBeeManager xbeeLink) {
        this.padLink = padLink;
        this.xbeeLink = xbeeLink;
    }

    private void send(String cmd) {
        if (padLink != null) padLink.send(cmd);
        if (xbeeLink != null && xbeeLink != padLink) xbeeLink.send(cmd);
    }

    public void showTarget(char side) { send(side == 'L' ? "P,LED,B" : "P,LED,Y"); send("P,BUZ,BEEP"); }
    public void correct()   { send("P,LED,G"); send("P,BUZ,OK"); send("G,VIB,150"); }
    public void wrong()     { send("P,LED,R"); send("P,BUZ,BAD"); }
    public void countdown() { send("P,BUZ,BEEP"); }
    public void go()        { send("P,BUZ,GO"); }
    public void off()       { send("P,LED,O"); }
}
