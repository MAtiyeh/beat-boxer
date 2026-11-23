package beatboxer;

/** Observer pattern: anything that wants sensor events implements this. */
public interface SensorListener {
    void onPacket(SensorPacket packet);
}
