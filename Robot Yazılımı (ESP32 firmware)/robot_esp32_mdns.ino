/*
 * IoT Rescue Robot - ESP32 Controller (mDNS/WiFi Station Mode)
 * 
 * Features:
 * - WiFi Station Mode (connects to existing WiFi)
 * - mDNS support (accessible via rescuerobot.local)
 * - TCP Server on port 8080
 * - Motor control (Forward, Backward, Left, Right, Stop)
 * - Auto/Manual mode switching
 * - BLE scanning for victim detection
 * - Ultrasonic obstacle detection
 * - Web status page
 */

#include <WiFi.h>
#include <ESPmDNS.h>
#include <BLEDevice.h>
#include <BLEScan.h>
#include <WebServer.h>

// ========== WiFi Configuration ==========
const char* ssid = "YOUR_WIFI_SSID";        // Change this
const char* password = "YOUR_WIFI_PASSWORD"; // Change this
const char* mdnsName = "rescuerobot";        // Access via rescuerobot.local

WiFiServer tcpServer(8080);
WiFiClient client;
WebServer webServer(80);

// ========== Motor Pins ==========
#define MOTOR_LEFT_FWD    25
#define MOTOR_LEFT_BWD    26
#define MOTOR_RIGHT_FWD   27
#define MOTOR_RIGHT_BWD   14

// ========== Sensor Pins ==========
#define ULTRASONIC_TRIG   5
#define ULTRASONIC_ECHO   18
#define VICTIM_DETECT_PIN 19

// ========== BLE Configuration ==========
BLEScan* pBLEScan;
int scanTime = 1;
bool isAutoMode = false;

// ========== Robot State ==========
enum RobotMode { MANUAL, AUTO };
RobotMode currentMode = MANUAL;
unsigned long lastBLEScan = 0;
const long BLE_SCAN_INTERVAL = 3000;

// ========== Statistics ==========
int totalCommands = 0;
int totalObstacles = 0;
int totalVictims = 0;

void setup() {
  Serial.begin(115200);
  delay(1000);
  
  // Initialize motor pins
  pinMode(MOTOR_LEFT_FWD, OUTPUT);
  pinMode(MOTOR_LEFT_BWD, OUTPUT);
  pinMode(MOTOR_RIGHT_FWD, OUTPUT);
  pinMode(MOTOR_RIGHT_BWD, OUTPUT);
  
  // Initialize sensor pins
  pinMode(ULTRASONIC_TRIG, OUTPUT);
  pinMode(ULTRASONIC_ECHO, INPUT);
  pinMode(VICTIM_DETECT_PIN, INPUT_PULLUP);
  
  stopMotors();
  
  // Connect to WiFi
  Serial.println("🚀 Starting Rescue Robot (mDNS Mode)...");
  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);
  
  Serial.print("Connecting to WiFi");
  int attempts = 0;
  while (WiFi.status() != WL_CONNECTED && attempts < 20) {
    delay(500);
    Serial.print(".");
    attempts++;
  }
  
  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("\n✅ WiFi Connected!");
    Serial.print("📡 IP Address: ");
    Serial.println(WiFi.localIP());
    
    // Start mDNS
    if (MDNS.begin(mdnsName)) {
      Serial.print("🌐 mDNS started: http://");
      Serial.print(mdnsName);
      Serial.println(".local");
      MDNS.addService("http", "tcp", 80);
      MDNS.addService("robot", "tcp", 8080);
    }
  } else {
    Serial.println("\n❌ WiFi connection failed!");
  }
  
  // Start TCP server
  tcpServer.begin();
  Serial.println("🌐 TCP Server started on port 8080");
  
  // Setup web server
  setupWebServer();
  webServer.begin();
  Serial.println("🌍 Web Server started on port 80");
  
  // Initialize BLE
  BLEDevice::init("RescueRobot");
  pBLEScan = BLEDevice::getScan();
  pBLEScan->setActiveScan(true);
  Serial.println("📶 BLE initialized");
  
  Serial.println("✅ Robot ready!");
}

void loop() {
  // Handle web requests
  webServer.handleClient();
  
  // Check for new TCP client
  if (!client.connected()) {
    client = tcpServer.available();
    if (client) {
      Serial.println("📱 TCP Client connected!");
    }
  }
  
  // Handle client commands
  if (client && client.connected() && client.available()) {
    char command = client.read();
    handleCommand(command);
  }
  
  // ========== RADAR: BLE scanning (ALWAYS ACTIVE) ==========
  // This runs in both AUTO and MANUAL modes for radar display
  if (millis() - lastBLEScan > BLE_SCAN_INTERVAL) {
    performBLEScan();
    lastBLEScan = millis();
  }
  
  // Auto mode navigation (ONLY in auto mode)
  if (isAutoMode) {
    autoNavigate(); // Separate function for autonomous navigation
  }
  
  // Check sensors
  checkObstacle();
  checkVictim();
  
  delay(50);
}

// ========== Web Server Setup ==========
void setupWebServer() {
  webServer.on("/", HTTP_GET, []() {
    String html = "<!DOCTYPE html><html><head>";
    html += "<title>Rescue Robot Status</title>";
    html += "<meta name='viewport' content='width=device-width, initial-scale=1'>";
    html += "<style>";
    html += "body { font-family: Arial; background: #121212; color: #fff; padding: 20px; }";
    html += "h1 { color: #00E676; }";
    html += ".card { background: #1E1E1E; padding: 20px; margin: 10px 0; border-radius: 8px; }";
    html += ".status { font-size: 24px; font-weight: bold; }";
    html += ".online { color: #00E676; }";
    html += ".offline { color: #FF5252; }";
    html += "</style></head><body>";
    html += "<h1>🤖 Rescue Robot Dashboard</h1>";
    
    html += "<div class='card'>";
    html += "<h2>Status</h2>";
    html += "<p class='status'>";
    html += currentMode == AUTO ? "<span class='online'>AUTO MODE</span>" : "<span class='online'>MANUAL MODE</span>";
    html += "</p>";
    html += "<p>WiFi: <span class='online'>Connected</span></p>";
    html += "<p>IP: " + WiFi.localIP().toString() + "</p>";
    html += "<p>mDNS: rescuerobot.local</p>";
    html += "</div>";
    
    html += "<div class='card'>";
    html += "<h2>Statistics</h2>";
    html += "<p>Total Commands: " + String(totalCommands) + "</p>";
    html += "<p>Obstacles Detected: " + String(totalObstacles) + "</p>";
    html += "<p>Victims Found: " + String(totalVictims) + "</p>";
    html += "</div>";
    
    html += "<div class='card'>";
    html += "<h2>Sensor Data</h2>";
    html += "<p>Distance: " + String(getDistance()) + " cm</p>";
    html += "</div>";
    
    html += "</body></html>";
    
    webServer.send(200, "text/html", html);
  });
  
  webServer.on("/stats", HTTP_GET, []() {
    String json = "{";
    json += "\"mode\":\"" + String(currentMode == AUTO ? "AUTO" : "MANUAL") + "\",";
    json += "\"commands\":" + String(totalCommands) + ",";
    json += "\"obstacles\":" + String(totalObstacles) + ",";
    json += "\"victims\":" + String(totalVictims) + ",";
    json += "\"distance\":" + String(getDistance());
    json += "}";
    
    webServer.send(200, "application/json", json);
  });
}

// ========== Command Handler ==========
void handleCommand(char cmd) {
  totalCommands++;
  Serial.print("📨 Command: ");
  Serial.println(cmd);
  
  switch(cmd) {
    case 'F':
      moveForward();
      sendToClient("MOVE:F");
      break;
      
    case 'B':
      moveBackward();
      sendToClient("MOVE:B");
      break;
      
    case 'L':
      turnLeft();
      sendToClient("MOVE:L");
      break;
      
    case 'R':
      turnRight();
      sendToClient("MOVE:R");
      break;
      
    case 'S':
      stopMotors();
      sendToClient("MOVE:S");
      break;
      
    case 'A':
      isAutoMode = true;
      currentMode = AUTO;
      Serial.println("🤖 AUTO MODE");
      sendToClient("MODE:AUTO");
      break;
      
    case 'M':
      isAutoMode = false;
      currentMode = MANUAL;
      stopMotors();
      Serial.println("👤 MANUAL MODE");
      sendToClient("MODE:MANUAL");
      break;
      
    default:
      Serial.println("❓ Unknown command");
      break;
  }
}

// ========== Motor Control ==========
void moveForward() {
  digitalWrite(MOTOR_LEFT_FWD, HIGH);
  digitalWrite(MOTOR_LEFT_BWD, LOW);
  digitalWrite(MOTOR_RIGHT_FWD, HIGH);
  digitalWrite(MOTOR_RIGHT_BWD, LOW);
  Serial.println("⬆️ Moving Forward");
}

void moveBackward() {
  digitalWrite(MOTOR_LEFT_FWD, LOW);
  digitalWrite(MOTOR_LEFT_BWD, HIGH);
  digitalWrite(MOTOR_RIGHT_FWD, LOW);
  digitalWrite(MOTOR_RIGHT_BWD, HIGH);
  Serial.println("⬇️ Moving Backward");
}

void turnLeft() {
  digitalWrite(MOTOR_LEFT_FWD, LOW);
  digitalWrite(MOTOR_LEFT_BWD, HIGH);
  digitalWrite(MOTOR_RIGHT_FWD, HIGH);
  digitalWrite(MOTOR_RIGHT_BWD, LOW);
  Serial.println("⬅️ Turning Left");
}

void turnRight() {
  digitalWrite(MOTOR_LEFT_FWD, HIGH);
  digitalWrite(MOTOR_LEFT_BWD, LOW);
  digitalWrite(MOTOR_RIGHT_FWD, LOW);
  digitalWrite(MOTOR_RIGHT_BWD, HIGH);
  Serial.println("➡️ Turning Right");
}

void stopMotors() {
  digitalWrite(MOTOR_LEFT_FWD, LOW);
  digitalWrite(MOTOR_LEFT_BWD, LOW);
  digitalWrite(MOTOR_RIGHT_FWD, LOW);
  digitalWrite(MOTOR_RIGHT_BWD, LOW);
  Serial.println("⏹️ Motors Stopped");
}

// ========== Auto Mode Navigation ==========
void autoNavigate() {
  long distance = getDistance();
  if (distance < 20 && distance > 0) {
    stopMotors();
    sendToClient("OBSTACLE");
    delay(200);
    
    turnRight();
    delay(500);
    stopMotors();
  } else {
    moveForward();
  }
}

// ========== BLE Scanning ==========
void performBLEScan() {
  Serial.println("📡 Scanning BLE...");
  BLEScanResults foundDevices = pBLEScan->start(scanTime, false);
  int count = foundDevices.getCount();
  
  Serial.print("Found ");
  Serial.print(count);
  Serial.println(" devices");
  
  for (int i = 0; i < count; i++) {
    BLEAdvertisedDevice device = foundDevices.getDevice(i);
    int rssi = device.getRSSI();
    
    String radarData = "RADAR:" + String(rssi);
    sendToClient(radarData);
    
    Serial.print("  Device: ");
    Serial.print(device.getAddress().toString().c_str());
    Serial.print(" RSSI: ");
    Serial.println(rssi);
  }
  
  pBLEScan->clearResults();
}

// ========== Ultrasonic Distance Sensor ==========
long getDistance() {
  digitalWrite(ULTRASONIC_TRIG, LOW);
  delayMicroseconds(2);
  digitalWrite(ULTRASONIC_TRIG, HIGH);
  delayMicroseconds(10);
  digitalWrite(ULTRASONIC_TRIG, LOW);
  
  long duration = pulseIn(ULTRASONIC_ECHO, HIGH, 30000);
  long distance = duration * 0.034 / 2;
  
  return distance;
}

// ========== Obstacle Detection ==========
void checkObstacle() {
  static unsigned long lastCheck = 0;
  if (millis() - lastCheck < 500) return;
  lastCheck = millis();
  
  long distance = getDistance();
  if (distance > 0 && distance < 15) {
    totalObstacles++;
    if (!isAutoMode) {
      sendToClient("OBSTACLE");
      Serial.println("⚠️ Obstacle detected!");
    }
  }
}

// ========== Victim Detection ==========
void checkVictim() {
  static unsigned long lastCheck = 0;
  if (millis() - lastCheck < 1000) return;
  lastCheck = millis();
  
  if (digitalRead(VICTIM_DETECT_PIN) == LOW) {
    totalVictims++;
    sendToClient("VICTIM");
    Serial.println("🚨 VICTIM DETECTED!");
    
    if (isAutoMode) {
      stopMotors();
      delay(2000);
    }
  }
}

// ========== Send Data to Client ==========
void sendToClient(String message) {
  if (client && client.connected()) {
    client.println(message);
    Serial.print("📤 Sent: ");
    Serial.println(message);
  }
}
