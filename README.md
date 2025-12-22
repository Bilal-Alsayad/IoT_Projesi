# IoT_Projesi  
**IoT-Based Smart Rescue Robot System**

This repository contains an IoT-based smart search and rescue robot developed for disaster scenarios such as earthquakes and building collapses.  
The system aims to detect victims using Bluetooth Low Energy (BLE), prioritize critical cases, and support rescue teams with real-time control and data-driven analysis.

---

## Project Overview

The Smart Rescue Robot is designed as a low-cost, portable, and autonomous IoT solution.  
It detects nearby victim smartphones via BLE advertising, estimates proximity using RSSI values, and prioritizes victims who activate an emergency **HELP** signal.  
Operational data is visualized in real time and stored in the cloud for post-mission analysis.

---

## System Architecture

The project consists of three main components:

- **Robot Firmware (ESP32)**
  - BLE scanning and priority-based target selection
  - Direction finding using RSSI and physical U-Shield isolation
  - Obstacle avoidance with ultrasonic sensor
  - Real-time communication via Wi-Fi TCP Socket

- **ControllerApp (Android / Kotlin)**
  - Manual and autonomous robot control
  - Real-time path visualization
  - Cloud data upload (Firebase)

- **VictimApp (Android / Kotlin)**
  - BLE advertising as a victim beacon
  - Emergency HELP mode (Priority UUID)
  - Inverse feedback (alarm & flashlight when robot approaches)

---

## Technologies Used

- **ESP32** – Microcontroller with integrated Wi-Fi and BLE  
- **Bluetooth Low Energy (BLE)** – Victim detection and prioritization  
- **Wi-Fi TCP Sockets** – Low-latency real-time control  
- **Firebase Realtime Database** – Cloud-based data storage and analysis  
- **Kotlin / Android** – Mobile application development  

---

## Repository Structure

