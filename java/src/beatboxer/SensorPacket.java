package beatboxer;

/** One message received from a node, e.g. "P,L" or "G,PUNCH,3.1,450". */
public class SensorPacket {
    public final String node;      // "P" = pad node, "G" = glove node
    public final String type;      // "L", "R", "PUNCH", "READY", ...
    public final String[] values;  // extra values after the type
    public final long timeMs;      // time the laptop received it
    public final String source;    // which serial port it came from

    public SensorPacket(String node, String type, String[] values, long timeMs, String source) {
        this.node = node;
        this.type = type;
        this.values = values;
        this.timeMs = timeMs;
        this.source = source;
    }

    public static SensorPacket parse(String line, String source) {
        String[] parts = line.trim().split(",");
        if (parts.length < 2) return null;
        String node = parts[0].trim();
        if (!node.equals("P") && !node.equals("G")) return null;
        String[] vals = new String[parts.length - 2];
        System.arraycopy(parts, 2, vals, 0, vals.length);
        return new SensorPacket(node, parts[1].trim(), vals, System.currentTimeMillis(), source);
    }

    public double valueAsDouble(int i, double def) {
        try { return Double.parseDouble(values[i]); } catch (Exception e) { return def; }
    }

    @Override public String toString() {
        return node + "," + type + (values.length > 0 ? "," + String.join(",", values) : "") + " (" + source + ")";
    }
}
