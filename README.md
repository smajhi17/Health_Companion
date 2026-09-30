# 🏥 MedNexus — Smart Health Companion
> **Smart India Hackathon (SIH) Project**  
> *Privacy-First, Zero-Latency Offline IoT & BLE Health Monitoring System*

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Java](https://img.shields.io/badge/Language-Java-007396?style=for-the-badge&logo=java&logoColor=white)
![ESP32](https://img.shields.io/badge/Hardware-ESP32%20BLE-E7352C?style=for-the-badge&logo=espressif&logoColor=white)
![Bluetooth LE](https://img.shields.io/badge/Wireless-BLE%20GATT-0082FC?style=for-the-badge&logo=bluetooth&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)

---

## 📌 Overview

**MedNexus** is an edge-computed, offline-first health companion application designed to deliver real-time vital sign monitoring and instant fall detection. Built for edge health devices powered by the **ESP32 microcontroller**, MedNexus operates entirely without internet access—ensuring zero latency and maximum patient privacy.

---

## ⚡ Key Features

- 🫀 **Live Vital Sign Telemetry:** Real-time monitoring of Heart Rate (BPM), Blood Oxygen (SpO2), Body Temperature (°C), and Humidity (%).
- 🚨 **Real-Time Fall Detection:** Instant visual alert (`⚠ POSSIBLE FALL`) triggered upon detecting abnormal acceleration vectors via MPU6050 sensor algorithms.
- 📶 **Robust BLE Scanning & Auto-Connect:** High-performance Bluetooth Low Energy (BLE GATT) connectivity with support for notification & indication descriptors (`CCCD 0x2902`).
- 🔄 **Flexible Multi-Format Data Parser:** Seamlessly parses JSON payloads, CSV data streams, Key=Value pairs, and raw sensor numbers transmitted by ESP32.
- 🧪 **Interactive SIH Demo Mode:** Built-in interactive sensor simulator with on-demand fall alert triggers for live presentations.
- 🌓 **Adaptive Light & Dark Theme:** Built with Material Design 3, dynamic notch cutout handling, and full dark mode compatibility.
- 🔒 **Zero-Internet Privacy Guarantee:** Operates 100% offline with zero cloud dependency to safeguard patient health data.

---

## 🛠️ Hardware & Architecture

```
┌──────────────────────────────────────┐          Bluetooth LE          ┌──────────────────────────────────┐
│          ESP32 Microcontroller       │  ────────────────────────────► │        MedNexus Android App      │
│  - MAX30102 (Heart Rate & SpO2)      │     GATT Service (0x2902)      │  - Custom BLE Manager            │
│  - DHT11/22 (Temp & Humidity)        │   Notification & Indication  │  - Adaptive Data Parser          │
│  - MPU6050 (Fall Alert Accelerometer)│                                │  - Real-Time Live Telemetry Dashboard│
└──────────────────────────────────────┘                                └──────────────────────────────────┘
```

---

## 📱 App Screenshots & Highlights

| Live Telemetry Dashboard | Dark Mode Theme | Interactive SIH Demo Mode |
| :---: | :---: | :---: |
| Real-time vital metrics display | Full Dark Theme & Notch Insets Support | Simulated sensor telemetry & Fall Alert trigger |

---

## 🔧 ESP32 Firmware Example (C++ / Arduino)

Upload this C++ sketch to your ESP32 board to transmit telemetry to the MedNexus app:

```cpp
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

#define SERVICE_UUID        "8d9556d3-7856-4758-a157-f92c1c596078"
#define CHARACTERISTIC_UUID "96bb7c8f-b727-46eb-bdc1-aa9dab3fff8e"

BLECharacteristic *pCharacteristic;
bool deviceConnected = false;

class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
    }
    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
      BLEDevice::startAdvertising();
    }
};

void setup() {
  Serial.begin(115200);
  BLEDevice::init("HEALTH_COMPANION");

  BLEServer *pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  BLEService *pService = pServer->createService(SERVICE_UUID);
  pCharacteristic = pService->createCharacteristic(
                      CHARACTERISTIC_UUID,
                      BLECharacteristic::PROPERTY_READ |
                      BLECharacteristic::PROPERTY_NOTIFY
                    );

  pCharacteristic->addDescriptor(new BLE2902());
  pService->start();

  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  BLEDevice::startAdvertising();

  Serial.println("MedNexus ESP32 Ready & Advertising!");
}

void loop() {
  if (deviceConnected) {
    // Send telemetry formatted as JSON
    String jsonPayload = "{\"heartRate\":75,\"spo2\":98,\"temperature\":36.6,\"humidity\":50.0,\"activity\":0,\"fall\":0}";
    pCharacteristic->setValue(jsonPayload.c_str());
    pCharacteristic->notify();
    delay(1500);
  }
}
```

---

## 🚀 Getting Started

### Prerequisites
* Android Studio Iguana / Jellyfish / 2024.1+
* Android device with Android 8.0 (API 26) or higher and Bluetooth LE enabled
* (Optional) ESP32 Microcontroller with sensors

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/MedNexus.git
   ```
2. Open the project in **Android Studio**.
3. Build and deploy to your Android device (`:app:assembleDebug`).
4. Tap **SCAN & CONNECT BLE DEVICE** to connect to ESP32, or tap **START DEMO MODE** for simulated live presentation testing.

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

---

<p align="center">
  <b>Developed for Smart India Hackathon (SIH)</b><br>
  <i>MedNexus — Empowering Privacy-First Healthcare</i>
</p>
