package com.example.healthcompanion.bluetooth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;

import androidx.core.content.ContextCompat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BleManager {

    public interface BleListener {

        void onDeviceFound(BluetoothDevice device);

        void onConnecting();

        void onConnected();

        void onDisconnected();

        void onDataReceived(String data);

        void onError(String message);
    }

    private final Context context;
    private final BleListener listener;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt bluetoothGatt;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private boolean scanning = false;
    private final List<String> discoveredDeviceNames = new ArrayList<>();

    private void handleScanTimeout() {
        if (scanning) {
            stopScan();
            if (discoveredDeviceNames.isEmpty()) {
                listener.onError("No BLE devices found nearby. Make sure ESP32 is powered on and advertising.");
            } else {
                listener.onError("No Health Companion device found. Devices seen nearby: " + discoveredDeviceNames);
            }
        }
    }

    private final Runnable stopScanRunnable = this::handleScanTimeout;

    public BleManager(
            Context context,
            BleListener listener
    ) {

        this.context = context.getApplicationContext();
        this.listener = listener;

        BluetoothManager bluetoothManager =
                (BluetoothManager)
                        context.getSystemService(
                                Context.BLUETOOTH_SERVICE
                        );

        if (bluetoothManager != null) {

            bluetoothAdapter =
                    bluetoothManager.getAdapter();
        }
    }

    public boolean isBluetoothSupported() {

        return bluetoothAdapter != null;
    }

    public boolean isBluetoothEnabled() {

        return bluetoothAdapter != null
                && bluetoothAdapter.isEnabled();
    }

    private boolean hasScanPermission() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.S) {

            return ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED;

        } else {

            return ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private boolean hasConnectPermission() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.S) {

            return ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED;
        }

        return true;
    }

    @SuppressLint("MissingPermission")
    public void startScan() {

        if (!hasScanPermission()) {

            listener.onError(
                    "Bluetooth scan permission not granted."
            );

            return;
        }

        if (!isBluetoothSupported()) {

            listener.onError(
                    "Bluetooth LE is not supported."
            );

            return;
        }

        if (!isBluetoothEnabled()) {

            listener.onError(
                    "Please turn on Bluetooth."
            );

            return;
        }

        if (scanning) {
            return;
        }

        bluetoothLeScanner =
                bluetoothAdapter.getBluetoothLeScanner();

        if (bluetoothLeScanner == null) {

            listener.onError(
                    "Unable to access Bluetooth scanner."
            );

            return;
        }

        discoveredDeviceNames.clear();
        scanning = true;

        ScanSettings settings =
                new ScanSettings.Builder()
                        .setScanMode(
                                ScanSettings.SCAN_MODE_LOW_LATENCY
                        )
                        .build();

        // Pass null for filters to avoid OEM hardware scan filter blocks
        bluetoothLeScanner.startScan(
                null,
                settings,
                scanCallback
        );

        handler.removeCallbacks(stopScanRunnable);
        handler.postDelayed(
                stopScanRunnable,
                10000
        );
    }

    @SuppressLint("MissingPermission")
    public void stopScan() {

        handler.removeCallbacks(stopScanRunnable);

        if (!scanning) {
            return;
        }

        if (bluetoothLeScanner != null
                && hasScanPermission()) {

            bluetoothLeScanner.stopScan(
                    scanCallback
            );
        }

        scanning = false;
    }

    private final ScanCallback scanCallback =
            new ScanCallback() {

                @Override
                public void onScanResult(
                        int callbackType,
                        ScanResult result
                ) {

                    if (result == null) {
                        return;
                    }

                    BluetoothDevice device =
                            result.getDevice();

                    if (device == null) {
                        return;
                    }

                    String advertisedName = null;
                    if (result.getScanRecord() != null) {
                        advertisedName = result.getScanRecord().getDeviceName();
                    }

                    @SuppressLint("MissingPermission")
                    String cachedName = hasConnectPermission() ? device.getName() : null;

                    String nameToUse = (advertisedName != null && !advertisedName.trim().isEmpty())
                            ? advertisedName.trim()
                            : (cachedName != null ? cachedName.trim() : null);

                    if (nameToUse != null && !nameToUse.isEmpty() && !discoveredDeviceNames.contains(nameToUse)) {
                        discoveredDeviceNames.add(nameToUse);
                    }

                    boolean isMatch = false;

                    if (nameToUse != null) {

                        String upper = nameToUse.toUpperCase(Locale.US);

                        if (upper.contains("HEALTH")
                                || upper.contains("COMPANION")
                                || upper.contains("ESP32")) {

                            isMatch = true;
                        }
                    }

                    if (!isMatch && result.getScanRecord() != null && result.getScanRecord().getServiceUuids() != null) {

                        for (ParcelUuid parcelUuid : result.getScanRecord().getServiceUuids()) {

                            if (BleConstants.HEALTH_SERVICE_UUID.equals(parcelUuid.getUuid())) {

                                isMatch = true;

                                break;
                            }
                        }
                    }

                    if (isMatch) {

                        stopScan();

                        listener.onDeviceFound(device);
                    }
                }

                @Override
                public void onScanFailed(
                        int errorCode
                ) {

                    scanning = false;

                    listener.onError(
                            "BLE scan failed. Error code: "
                                    + errorCode
                    );
                }
            };

    @SuppressLint("MissingPermission")
    public void connect(BluetoothDevice device) {

        if (!hasConnectPermission()) {

            listener.onError(
                    "Bluetooth connection permission not granted."
            );

            return;
        }

        if (device == null) {

            listener.onError(
                    "Bluetooth device is null."
            );

            return;
        }

        listener.onConnecting();

        // Close any existing GATT connection first
        closeGatt();

        bluetoothGatt =
                device.connectGatt(
                        context,
                        false,
                        gattCallback,
                        BluetoothDevice.TRANSPORT_LE
                );
    }

    private final BluetoothGattCallback gattCallback =
            new BluetoothGattCallback() {

                @Override
                public void onConnectionStateChange(
                        BluetoothGatt gatt,
                        int status,
                        int newState
                ) {

                    if (status != BluetoothGatt.GATT_SUCCESS) {

                        listener.onError("GATT connection failed (Status: " + status + ")");

                        listener.onDisconnected();

                        closeGatt();

                        return;
                    }

                    if (newState ==
                            BluetoothGatt.STATE_CONNECTED) {

                        listener.onConnected();

                        // Short delay before service discovery ensures connection stability
                        handler.postDelayed(() -> {

                            if (hasConnectPermission() && bluetoothGatt != null) {

                                bluetoothGatt.discoverServices();
                            }
                        }, 300);

                    } else if (newState ==
                            BluetoothGatt.STATE_DISCONNECTED) {

                        listener.onDisconnected();

                        closeGatt();
                    }
                }

                @Override
                public void onServicesDiscovered(
                        BluetoothGatt gatt,
                        int status
                ) {

                    if (status !=
                            BluetoothGatt.GATT_SUCCESS) {

                        listener.onError(
                                "Service discovery failed with status: " + status
                        );

                        return;
                    }

                    BluetoothGattService service =
                            gatt.getService(
                                    BleConstants
                                            .HEALTH_SERVICE_UUID
                            );

                    BluetoothGattCharacteristic characteristic = null;

                    if (service != null) {

                        characteristic =
                                service.getCharacteristic(
                                        BleConstants
                                                .HEALTH_DATA_CHARACTERISTIC_UUID
                                );
                    }

                    // Fallback: search all services on ESP32 if exact UUID match was not found
                    if (characteristic == null) {

                        for (BluetoothGattService s : gatt.getServices()) {

                            for (BluetoothGattCharacteristic c : s.getCharacteristics()) {

                                if (BleConstants.HEALTH_DATA_CHARACTERISTIC_UUID.equals(c.getUuid())
                                        || (c.getProperties() & (BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_INDICATE)) != 0) {

                                    service = s;
                                    characteristic = c;
                                    break;
                                }
                            }

                            if (characteristic != null) {
                                break;
                            }
                        }
                    }

                    if (service == null || characteristic == null) {

                        StringBuilder sb = new StringBuilder("Service/Characteristic not found on ESP32.\nServices found:");

                        for (BluetoothGattService s : gatt.getServices()) {
                            sb.append("\n- ").append(s.getUuid());
                        }

                        listener.onError(sb.toString());

                        return;
                    }

                    enableNotifications(
                            gatt,
                            characteristic
                    );
                }

                @Override
                public void onCharacteristicChanged(
                        BluetoothGatt gatt,
                        BluetoothGattCharacteristic characteristic
                ) {

                    if (characteristic != null) {

                        processData(
                                characteristic.getValue()
                        );
                    }
                }

                @Override
                public void onCharacteristicChanged(
                        BluetoothGatt gatt,
                        BluetoothGattCharacteristic characteristic,
                        byte[] value
                ) {

                    if (characteristic != null) {

                        processData(
                                value
                        );
                    }
                }

                @Override
                public void onCharacteristicRead(
                        BluetoothGatt gatt,
                        BluetoothGattCharacteristic characteristic,
                        int status
                ) {

                    if (status == BluetoothGatt.GATT_SUCCESS && characteristic != null) {

                        processData(
                                characteristic.getValue()
                        );
                    }
                }

                @Override
                public void onCharacteristicRead(
                        BluetoothGatt gatt,
                        BluetoothGattCharacteristic characteristic,
                        byte[] value,
                        int status
                ) {

                    if (status == BluetoothGatt.GATT_SUCCESS && characteristic != null) {

                        processData(
                                value
                        );
                    }
                }

                private void processData(
                        byte[] value
                ) {

                    if (value == null || value.length == 0) {
                        return;
                    }

                    String data =
                            new String(
                                    value,
                                    StandardCharsets.UTF_8
                            );

                    listener.onDataReceived(data);
                }
            };

    @SuppressLint("MissingPermission")
    private void enableNotifications(
            BluetoothGatt gatt,
            BluetoothGattCharacteristic characteristic
    ) {

        if (!hasConnectPermission()) {
            return;
        }

        boolean isNotify = (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
        boolean isIndicate = (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0;

        boolean notificationEnabled =
                gatt.setCharacteristicNotification(
                        characteristic,
                        true
                );

        if (!notificationEnabled) {

            listener.onError(
                    "Could not enable BLE notifications on Android."
            );

            return;
        }

        BluetoothGattDescriptor descriptor =
                characteristic.getDescriptor(
                        BleConstants.CCCD_UUID
                );

        if (descriptor == null) {

            for (BluetoothGattDescriptor desc : characteristic.getDescriptors()) {

                if (desc.getUuid() != null && desc.getUuid().toString().startsWith("00002902")) {
                    descriptor = desc;
                    break;
                }
            }
        }

        if (descriptor == null) {

            listener.onError(
                    "CCCD notification descriptor (0x2902) not found."
            );

            return;
        }

        byte[] descriptorValue = isIndicate ?
                BluetoothGattDescriptor.ENABLE_INDICATION_VALUE :
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;

        descriptor.setValue(descriptorValue);

        boolean descriptorWritten;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            int result = gatt.writeDescriptor(
                    descriptor,
                    descriptorValue
            );

            descriptorWritten = (result == BluetoothGatt.GATT_SUCCESS);

        } else {

            descriptorWritten = gatt.writeDescriptor(descriptor);
        }

        if (!descriptorWritten) {

            listener.onError(
                    "Could not write BLE notification descriptor."
            );

        } else {

            // Read initial characteristic value if supported
            if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0) {

                final BluetoothGattCharacteristic charToRead = characteristic;

                handler.postDelayed(() -> {

                    if (hasConnectPermission() && bluetoothGatt != null) {

                        bluetoothGatt.readCharacteristic(charToRead);
                    }
                }, 200);
            }
        }
    }

    @SuppressLint("MissingPermission")
    public void disconnect() {

        if (bluetoothGatt != null
                && hasConnectPermission()) {

            bluetoothGatt.disconnect();
        }
    }

    @SuppressLint("MissingPermission")
    public void closeGatt() {

        if (bluetoothGatt != null) {

            if (hasConnectPermission()) {

                bluetoothGatt.close();
            }

            bluetoothGatt = null;
        }
    }
}
