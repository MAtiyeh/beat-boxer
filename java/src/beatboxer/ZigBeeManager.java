package beatboxer;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Opens one serial port (the XBee coordinator, or the pad node's USB cable),
 * splits incoming bytes into lines, turns them into SensorPackets and notifies
 * all listeners (Observer pattern). Also sends text commands back.
 */
public class ZigBeeManager {
    private final String portName;
    private SerialPort port;
    private final List<SensorListener> listeners = new CopyOnWriteArrayList<>();
    private final StringBuilder buffer = new StringBuilder();
    private volatile String lastLine = "";

    public ZigBeeManager(String portName) {
        this.portName = portName;
    }

    public String getPortName() { return portName; }
    public String getLastLine() { return lastLine; }

    public void addListener(SensorListener l) { listeners.add(l); }

    public boolean open() {
        port = SerialPort.getCommPort(portName);
        port.setComPortParameters(9600, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        port.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0);
        if (!port.openPort()) return false;
        port.addDataListener(new SerialPortDataListener() {
            @Override public int getListeningEvents() { return SerialPort.LISTENING_EVENT_DATA_AVAILABLE; }
            @Override public void serialEvent(SerialPortEvent event) {
                if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) return;
                int n = port.bytesAvailable();
                if (n <= 0) return;
                byte[] data = new byte[n];
                int read = port.readBytes(data, n);
                handleBytes(new String(data, 0, Math.max(read, 0), StandardCharsets.US_ASCII));
            }
        });
        return true;
    }

    private void handleBytes(String text) {
        for (char c : text.toCharArray()) {
            if (c == '\n' || c == '\r') {
                if (buffer.length() > 0) {
                    String line = buffer.toString();
                    buffer.setLength(0);
                    lastLine = line;
                    SensorPacket p = SensorPacket.parse(line, portName);
                    if (p != null) for (SensorListener l : listeners) l.onPacket(p);
                }
            } else if (buffer.length() < 100) {
                buffer.append(c);
            }
        }
    }

    public void send(String command) {
        if (port == null || !port.isOpen()) return;
        byte[] b = (command + "\n").getBytes(StandardCharsets.US_ASCII);
        port.writeBytes(b, b.length);
    }

    public boolean isOpen() { return port != null && port.isOpen(); }

    public void close() {
        if (port != null) {
            port.removeDataListener();
            port.closePort();
        }
    }
}
