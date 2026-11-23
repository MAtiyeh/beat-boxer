// ===== Beat Boxer - GLOVE NODE =====
// MPU-6050: VCC -> +, GND -> -, SCL -> a5, SDA -> a4
// Vibe board: + -> 6, - -> -
// XBee board: + -> +, - -> -, rx -> 3
// Sends: G,READY   and   G,PUNCH,<peak g>,<peak gyro dps>
#include <Wire.h>
#include <SoftwareSerial.h>

const int VIBE = 6;
int MPU = 0x68;
SoftwareSerial xbee(2, 3); // RX (unused), TX -> XBee rx

const float PUNCH_G = 2.2;            // acceleration needed for a punch (in g)
const unsigned long COOLDOWN_MS = 350; // ignore new punches for this long
const unsigned long VIBE_MS = 150;

unsigned long lastPunch = 0;
unsigned long vibeOffAt = 0;
unsigned long lastPrint = 0;
float peakG = 0;

void mpuWrite(byte reg, byte val) {
  Wire.beginTransmission(MPU);
  Wire.write(reg);
  Wire.write(val);
  Wire.endTransmission();
}

byte mpuRead8(byte reg) {
  Wire.beginTransmission(MPU);
  Wire.write(reg);
  Wire.endTransmission(false);
  Wire.requestFrom(MPU, 1);
  return Wire.available() ? Wire.read() : 0xFF;
}

int16_t read16() {
  byte hi = Wire.read();
  byte lo = Wire.read();
  return (int16_t)((hi << 8) | lo);
}

bool readMotion(float &g, float &dps) {
  Wire.beginTransmission(MPU);
  Wire.write(0x3B);
  if (Wire.endTransmission(false) != 0) return false;
  Wire.requestFrom(MPU, 14);
  if (Wire.available() < 14) return false;
  int16_t ax = read16();
  int16_t ay = read16();
  int16_t az = read16();
  Wire.read(); Wire.read(); // temperature
  int16_t gx = read16();
  int16_t gy = read16();
  int16_t gz = read16();
  float fx = ax / 4096.0, fy = ay / 4096.0, fz = az / 4096.0;   // +-8g range
  float rx = gx / 32.8, ry = gy / 32.8, rz = gz / 32.8;         // +-1000 dps range
  g = sqrt(fx * fx + fy * fy + fz * fz);
  dps = sqrt(rx * rx + ry * ry + rz * rz);
  return true;
}

void sendMsg(const String &msg) {
  Serial.println(msg);
  xbee.println(msg);
}

void setup() {
  Serial.begin(9600);
  xbee.begin(9600);
  pinMode(VIBE, OUTPUT);
  digitalWrite(VIBE, LOW);
  Serial.println("Glove starting...");
  Wire.begin();
  Wire.setWireTimeout(3000, true); // never hang if the sensor is not answering
  Serial.print("I2C scan:");
  int found = 0;
  for (byte a = 1; a < 127; a++) {
    Wire.beginTransmission(a);
    if (Wire.endTransmission() == 0) { Serial.print(" 0x"); Serial.print(a, HEX); found++; if (a == 0x68 || a == 0x69) MPU = a; }
  }
  if (found == 0) Serial.print(" NOTHING FOUND");
  Serial.println();
  mpuWrite(0x6B, 0x00); // wake up
  mpuWrite(0x1C, 0x10); // accel +-8g
  mpuWrite(0x1B, 0x10); // gyro +-1000 dps
  delay(100);
  byte who = mpuRead8(0x75);
  Serial.print("MPU WHO_AM_I = 0x");
  Serial.println(who, HEX);
  if (who != 0x68 && who != 0x98 && who != 0x70) Serial.println("MPU NOT FOUND - check SDA/SCL clips");
  digitalWrite(VIBE, HIGH); delay(200); digitalWrite(VIBE, LOW);
  sendMsg("G,READY");
}

void loop() {
  float g, dps;
  unsigned long now = millis();
  bool ok = readMotion(g, dps);
  if (!ok && now - lastPrint > 1000) Serial.println("MPU read failed - check clips");
  if (ok) {
    if (g > peakG) peakG = g;
    if (g > PUNCH_G && now - lastPunch > COOLDOWN_MS) {
      lastPunch = now;
      sendMsg("G,PUNCH," + String(g, 1) + "," + String((int)dps));
      digitalWrite(VIBE, HIGH);
      vibeOffAt = now + VIBE_MS;
    }
  }
  if (vibeOffAt && now > vibeOffAt) { digitalWrite(VIBE, LOW); vibeOffAt = 0; }
  if (now - lastPrint > 1000) {          // debug: strongest movement in the last second
    Serial.print("peak g: ");
    Serial.println(peakG, 1);
    peakG = 0;
    lastPrint = now;
  }
  delay(5);
}
