// ===== Beat Boxer - GLOVE NODE (LilyPad accelerometer version) =====
// LilyPad Accelerometer (ADXL335, 3.3V part!):
//   + -> "3.3V" hole on the LilyPad XBee board   (NOT the 5V + of the LilyPad)
//   - -> LilyPad -
//   X -> a0,  Y -> a1,  Z -> a2
// Vibe board: + -> 6, - -> -
// XBee board: + -> +, - -> -, rx -> 3
// Sends: G,READY   and   G,PUNCH,<peak g>
#include <SoftwareSerial.h>

const int VIBE = 6;
SoftwareSerial xbee(2, 3); // RX (unused), TX -> XBee rx

// ADXL335 at 3.3V: 0 g = 1.65 V, 0.33 V per g. Arduino ADC: 5 V = 1023.
const float ZERO_G = 1.65 / 5.0 * 1023.0;   // ~338
const float PER_G  = 0.33 / 5.0 * 1023.0;   // ~67.5 counts per g

const float PUNCH_G = 2.0;              // movement needed for a punch (in g)
const unsigned long COOLDOWN_MS = 350;
const unsigned long VIBE_MS = 150;

float offX = 0, offY = 0, offZ = 0;      // calibration at rest
unsigned long lastPunch = 0, vibeOffAt = 0, lastPrint = 0;
float peakG = 0;

float axisG(int pin) { return (analogRead(pin) - ZERO_G) / PER_G; }

void sendMsg(const String &msg) {
  Serial.println(msg);
  xbee.println(msg);
}

void setup() {
  Serial.begin(9600);
  xbee.begin(9600);
  pinMode(VIBE, OUTPUT);
  digitalWrite(VIBE, LOW);
  Serial.println("Glove (accelerometer) starting - keep the glove still...");
  delay(300);
  // calibrate: measure the resting reading (gravity) so we only react to movement
  for (int i = 0; i < 50; i++) { offX += axisG(A0); offY += axisG(A1); offZ += axisG(A2); delay(10); }
  offX /= 50; offY /= 50; offZ /= 50;
  Serial.print("rest g: x="); Serial.print(offX, 2);
  Serial.print(" y="); Serial.print(offY, 2);
  Serial.print(" z="); Serial.println(offZ, 2);
  digitalWrite(VIBE, HIGH); delay(200); digitalWrite(VIBE, LOW);
  sendMsg("G,READY");
}

void loop() {
  unsigned long now = millis();
  float dx = axisG(A0) - offX, dy = axisG(A1) - offY, dz = axisG(A2) - offZ;
  float g = sqrt(dx * dx + dy * dy + dz * dz);   // movement on top of gravity
  if (g > peakG) peakG = g;
  if (g > PUNCH_G && now - lastPunch > COOLDOWN_MS) {
    lastPunch = now;
    sendMsg("G,PUNCH," + String(g, 1));
    digitalWrite(VIBE, HIGH);
    vibeOffAt = now + VIBE_MS;
  }
  if (vibeOffAt && now > vibeOffAt) { digitalWrite(VIBE, LOW); vibeOffAt = 0; }
  if (now - lastPrint > 1000) {
    Serial.print("peak g: "); Serial.println(peakG, 1);
    peakG = 0;
    lastPrint = now;
  }
  delay(2);
}
