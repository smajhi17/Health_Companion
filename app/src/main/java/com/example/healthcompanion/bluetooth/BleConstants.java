package com.example.healthcompanion.bluetooth;

import java.util.UUID;

public final class BleConstants {

    private BleConstants() {
        // Prevent object creation
    }

    /*
     * Client Characteristic Configuration Descriptor (CCCD)
     */
    public static final UUID CCCD_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    /*
     * Custom Health Companion BLE Service
     */
    public static final UUID HEALTH_SERVICE_UUID =
            UUID.fromString(
                    "8d9556d3-7856-4758-a157-f92c1c596078"
            );

    /*
     * Characteristic used by ESP32
     * to send health data to Android.
     */
    public static final UUID HEALTH_DATA_CHARACTERISTIC_UUID =
            UUID.fromString(
                    "96bb7c8f-b727-46eb-bdc1-aa9dab3fff8e"
            );

    /*
     * ESP32 device name.
     */
    public static final String DEVICE_NAME =
            "HEALTH_COMPANION";
}
