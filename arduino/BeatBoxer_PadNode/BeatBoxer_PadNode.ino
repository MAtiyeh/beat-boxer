// ===== Beat Boxer - PAD NODE =====
// Pads:   L -> a2 and -,  R -> a3 and -
// RGB LED (common +): + -> +, R -> 9, G -> 11, B -> 10
// Buzzer (low-level trigger): GND -> -, VCC -> +, I/O -> 5
// XBee board: + -> +, - -> -, XBee rx -> 3, XBee tx -> 2
// Sends:    P,L   or   P,R   when a pad is hit
// Receives: P,LED,R / P,LED,G / P,LED,B / P,LED,Y / P,LED,O(off)
//           P,BUZ,BEEP / P,BUZ,OK / P,BUZ,BAD / P,BUZ,GO
#include <SoftwareSerial.h>

const int PAD_L = A2;
const int PAD_R = A3;
const int LED_R = 9;
const int LED_G = 11;
const int LED_B = 10;
const int BUZ = 5;

SoftwareSerial xbee(2, 3); // RX, TX

const unsigned long MIN_GAP_MS = 250;
const unsigned long RELEASE_MS = 60;

struct Pad {
  int pin;
  bool armed;
  unsigned long lastHit;
  unsigned long releasedSince;
};
Pad padL = {PAD_L, true, 0, 0};
Pad padR = {PAD_R, true, 0, 0};

String bufUsb = "";
String bufXb = "";

bool checkPad(Pad &p) {
  bool pressed = digitalRead(p.pin) == LOW;
  unsigned long now = millis();
  if (pressed) {
    p.releasedSince = 0;
    if (p.armed && now - p.lastHit > MIN_GAP_MS) {
      p.armed = false;
      p.lastHit = now;
      return true;
    }
  } else {
    if (p.releasedSince == 0) p.releasedSince = now;
    if (!p.armed && now - p.releasedSince > RELEASE_MS) p.armed = true;
  }
  return false;
}

void setColor(bool r, bool g, bool b) {
  digitalWrite(LED_R, r ? LOW : HIGH);
  digitalWrite(LED_G, g ? LOW : HIGH);
  digitalWrite(LED_B, b ? LOW : HIGH);
}

void beep(int freq, int ms) {
  long half = 500000L / freq;
  long cycles = (long)ms * 1000L / (half * 2);
  for (long i = 0; i < cycles; i++) {
    digitalWrite(BUZ, LOW);
    delayMicroseconds(half);
    digitalWrite(BUZ, HIGH);
    delayMicroseconds(half);
  }
  digitalWrite(BUZ, HIGH);
}

void sendMsg(const char *msg) {
  Serial.println(msg);
  xbee.println(msg);
}

void handle(String cmd) {
  cmd.trim();
  if (!cmd.startsWith("P,")) return;
  if (cmd == "P,LED,R") setColor(true, false, false);
  else if (cmd == "P,LED,G") setColor(false, true, false);
  else if (cmd == "P,LED,B") setColor(false, false, true);
  else if (cmd == "P,LED,Y") setColor(true, true, false);
  else if (cmd == "P,LED,O") setColor(false, false, false);
  else if (cmd == "P,BUZ,BEEP") beep(1500, 60);
  else if (cmd == "P,BUZ,OK") { beep(2000, 70); beep(2700, 90); }
  else if (cmd == "P,BUZ,BAD") beep(400, 250);
  else if (cmd == "P,BUZ,GO") beep(3000, 300);
}

bool DEBUG_XB = false;

void readLines(Stream &s, String &buf, bool fromXb) {
  while (s.available()) {
    char c = s.read();
    if (fromXb && DEBUG_XB) { Serial.print("[XB "); Serial.print((int)c); Serial.println("]"); }
    if (c == '\n' || c == '\r') {
      if (buf.length() > 0) handle(buf);
      buf = "";
    } else if (buf.length() < 30) {
      buf += c;
    }
  }
}

void setup() {
  Serial.begin(9600);
  xbee.begin(9600);
  pinMode(PAD_L, INPUT_PULLUP);
  pinMode(PAD_R, INPUT_PULLUP);
  pinMode(LED_R, OUTPUT);
  pinMode(LED_G, OUTPUT);
  pinMode(LED_B, OUTPUT);
  pinMode(BUZ, OUTPUT);
  digitalWrite(BUZ, HIGH);
  setColor(true, false, false); delay(250);
  setColor(false, true, false); delay(250);
  setColor(false, false, true); delay(250);
  setColor(false, false, false);
  beep(2000, 80);
  sendMsg("P,READY");
}

void loop() {
  if (checkPad(padL)) sendMsg("P,L");
  if (checkPad(padR)) sendMsg("P,R");
  readLines(Serial, bufUsb, false);
  readLines(xbee, bufXb, true);
}
