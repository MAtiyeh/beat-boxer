# Beat Boxer

COE 421L final project. A rhythm reaction game: two punch pads, a sensor glove, XBee radios and a Java program that runs the game.

## How it works

- **Pad node** (LilyPad Arduino 328): two foil contact pads, RGB LED, buzzer, XBee. Sends `P,L` / `P,R` when a pad is hit.
- **Glove node** (LilyPad Arduino 328): LilyPad accelerometer, vibe board, XBee. Sends `G,PUNCH,<g>` when it detects a punch.
- **Coordinator**: XBee on an XBee Explorer USB, plugged into the laptop.
- **Java program**: picks targets, checks timing, keeps score, draws the screen and sends LED/buzzer commands.

Messages are ASCII text lines at 9600 baud, comma-separated, newline-terminated. Sensor events travel over the radios. LED and buzzer commands (`P,LED,B`, `P,BUZ,OK`, ...) are sent over the pad node's USB serial cable.

## Repository layout

| Path | Contents |
|---|---|
| `java/src/beatboxer/` | Java source |
| `arduino/BeatBoxer_PadNode/` | Pad node sketch |
| `arduino/BeatBoxer_Glove_Accel/` | Glove sketch (LilyPad accelerometer) |
| `arduino/BeatBoxer_Glove_MPU6050/` | Earlier glove sketch for an MPU-6050 (not used in the final build) |

## Wiring

**Pad node**

| From | LilyPad petal |
|---|---|
| Pad L wires | a2, − |
| Pad R wires | a3, − |
| RGB LED +, R, G, B | +, 9, 11, 10 |
| Buzzer VCC, GND, I/O | +, −, 5 |
| LilyPad XBee +, −, rx | +, −, 3 |

**Glove node**

| From | To |
|---|---|
| Accelerometer + | 3.3V petal of the LilyPad XBee board |
| Accelerometer − | LilyPad − |
| Accelerometer X, Y, Z | a0, a1, a2 |
| Vibe board +, − | 6, − |
| LilyPad XBee +, −, rx | +, −, 3 |

## XBee settings (XBee-PRO 802.15.4, AT mode, 9600 baud)

| Module | CE | MY | DL | PAN ID | CH |
|---|---|---|---|---|---|
| Coordinator | Coordinator | 1111 | FFFF | 1113 | C |
| Node 1 | End device | 1 | 1111 | 1113 | C |
| Node 2 | End device | 2 | 1111 | 1113 | C |

## Setup

1. Upload each sketch with the Arduino IDE: board **LilyPad Arduino**, processor **ATmega328P**, through an FTDI Basic board.
2. Install a Java JDK (17 or newer) and download `jSerialComm-2.11.0.jar` from <https://github.com/Fazecast/jSerialComm/releases>.
3. Build and run from the `java` folder:

   ```
   javac -d out -cp jSerialComm-2.11.0.jar src/beatboxer/*.java
   java -cp "out;jSerialComm-2.11.0.jar" beatboxer.BeatBoxerGame
   ```

   (On macOS/Linux use `:` instead of `;` in the classpath.)
4. Close XCTU and the Arduino Serial Monitor first. In the start dialog pick the coordinator's port and the pad node's USB port.

## Playing

- `1` / `2` / `3`: Easy / Medium / Hard. `G`: require a glove punch for each hit.
- Punch any pad (or press SPACE) to start. Blue LED = hit left, yellow = hit right.
- 20 targets per round. Keyboard test keys: `A` = left pad, `L` = right pad.
