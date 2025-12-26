/*
 * IoT Rescue Robot - ESP32 Controller (Access Point Mode)
 * 
 * Features:
 * - WiFi Access Point (SSID: RescueRobot, IP: 192.168.4.1)
 * - TCP Server on port 8080
 * - Motor control (Forward, Backward, Left, Right, Stop)
 * - Auto/Manual mode switching
 * - BLE scanning for victim detection
 * - Ultrasonic obstacle detection
 * - Real-time telemetry
 */

#include <WiFi.h>
#include <BLEDevice.h>
#include <BLEScan.h>

// ========== WiFi Configuration ==========
const char* ssid = "RescueRobot";
const char* password = "12345678";
WiFiServer server(8080);
WiFiClient client;

// ========== Motor Pins ==========
#define MOTOR_LEFT_FWD    25
#define MOTOR_LEFT_BWD    26
#define MOTOR_RIGHT_FWD   27
#define MOTOR_RIGHT_BWD   14

// ========== Sensor Pins ==========
#define ULTRASONIC_TRIG   5
#define ULTRASONIC_ECHO   18
#define VICTIM_DETECT_PIN 19  // Simulated victim detection

// ========== BLE Configuration ==========
BLEScan* pBLEScan;
int scanTime = 1; // BLE scan duration in seconds
bool isAutoMode = false;

// ========== Robot State ==========
enum RobotMode { MANUAL, AUTO };
RobotMode currentMode = MANUAL;
unsigned long lastBLEScan = 0;
const long BLE_SCAN_INTERVAL = 3000; // 3 seconds

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
  
  // Stop all motors
  stopMotors();
  
  // Setup WiFi Access Point
  Serial.println("🚀 Starting Rescue Robot...");
  WiFi.mode(WIFI_AP);
  WiFi.softAP(ssid, password);
  
  IPAddress IP = WiFi.softAPIP();
  Serial.print("📡 AP IP: ");
  Serial.println(IP);
  
  // Start TCP server
  server.begin();
  Serial.println("🌐 TCP Server started on port 8080");
  
  // Initialize BLE
  BLEDevice::init("RescueRobot");
  pBLEScan = BLEDevice::getScan();
  pBLEScan->setActiveScan(true);
  Serial.println("📶 BLE initialized");
  
  Serial.println("✅ Robot ready!");
}

void loop() {
  // Check for new client connection
  if (!client.connected()) {
    client = server.available();
    if (client) {
      Serial.println("📱 Client connected!");
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
  
  // Check for obstacles
  checkObstacle();
  
  // Check for victim
  checkVictim();
  
  delay(50);
}

// ========== Command Handler ==========
void handleCommand(char cmd) {
  Serial.print("📨 Command: ");
  Serial.println(cmd);
  
  switch(cmd) {
    case 'F': // Forward
      moveForward();
      sendToClient("MOVE:F");
      break;
      
    case 'B': // Backward
      moveBackward();
      sendToClient("MOVE:B");
      break;
      
    case 'L': // Left
      turnLeft();
      sendToClient("MOVE:L");
      break;
      
    case 'R': // Right
      turnRight();
      sendToClient("MOVE:R");
      break;
      
    case 'S': // Stop
      stopMotors();
      sendToClient("MOVE:S");
      break;
      
    case 'A': // Auto mode
      isAutoMode = true;
      currentMode = AUTO;
      Serial.println("🤖 AUTO MODE");
      sendToClient("MODE:AUTO");
      break;
      
    case 'M': // Manual mode
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
  // Simple auto navigation (obstacle avoidance)
  long distance = getDistance();
  if (distance < 20 && distance > 0) { // Less than 20cm
    stopMotors();
    sendToClient("OBSTACLE");
    delay(200);
    
    // Turn right to avoid
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
    
    // Send radar data
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
  
  long duration = pulseIn(ULTRASONIC_ECHO, HIGH, 30000); // 30ms timeout
  long distance = duration * 0.034 / 2; // Convert to cm
  
  return distance;
}

// ========== Obstacle Detection ==========
void checkObstacle() {
  static unsigned long lastCheck = 0;
  if (millis() - lastCheck < 500) return; // Check every 500ms
  lastCheck = millis();
  
  long distance = getDistance();
  if (distance > 0 && distance < 15) { // Less than 15cm
    if (!isAutoMode) {
      sendToClient("OBSTACLE");
      Serial.println("⚠️ Obstacle detected!");
    }
  }
}

// ========== Victim Detection ==========
void checkVictim() {
  static unsigned long lastCheck = 0;
  if (millis() - lastCheck < 1000) return; // Check every 1 second
  lastCheck = millis();
  
  // Check victim detection pin (active LOW)
  if (digitalRead(VICTIM_DETECT_PIN) == LOW) {
    sendToClient("VICTIM");
    Serial.println("🚨 VICTIM DETECTED!");
    
    // Stop if in auto mode
    if (isAutoMode) {
      stopMotors();
      delay(2000); // Pause for 2 seconds
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
