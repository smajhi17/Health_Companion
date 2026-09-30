package com.example.healthcompanion;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.healthcompanion.bluetooth.BleManager;
import com.example.healthcompanion.model.HealthData;
import com.example.healthcompanion.utils.DataParser;

import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity
        implements BleManager.BleListener {

    private TextView connectionStatus;
    private TextView heartRateValue;
    private TextView spo2Value;
    private TextView temperatureValue;
    private TextView humidityValue;
    private TextView activityValue;
    private TextView fallValue;

    private Button scanButton;
    private Button demoButton;
    private Button fallSimButton;
    private Button disconnectButton;

    private BleManager bleManager;

    // Demo Mode Handler & Variables
    private boolean isDemoMode = false;
    private boolean simulatedFallTriggered = false;
    private final Handler demoHandler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    private final Runnable demoRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isDemoMode) return;

            int hr = 72 + random.nextInt(12);               // 72 - 84 BPM
            int spo2 = 97 + random.nextInt(3);              // 97 - 99 %
            float temp = 36.4f + random.nextFloat() * 0.6f;  // 36.4 - 37.0 °C
            float hum = 48.0f + random.nextFloat() * 4.0f;   // 48 - 52 %
            int activity = random.nextInt(10) > 7 ? 1 : 0;
            int fall = simulatedFallTriggered ? 1 : 0;

            HealthData data = new HealthData();
            data.setHeartRate(hr);
            data.setSpo2(spo2);
            data.setTemperature(temp);
            data.setHumidity(hum);
            data.setActivity(activity);
            data.setFall(fall);

            updateDashboard(data);

            if (simulatedFallTriggered) {
                simulatedFallTriggered = false;
            }

            demoHandler.postDelayed(this, 1500);
        }
    };

    private final ActivityResultLauncher<String[]>
            permissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts
                            .RequestMultiplePermissions(),
                    result -> {

                        boolean granted = true;

                        for (Boolean value :
                                result.values()) {

                            if (!Boolean.TRUE.equals(value)) {
                                granted = false;
                                break;
                            }
                        }

                        if (granted) {

                            startBleScan();

                        } else {

                            showToast(
                                    "Bluetooth permission is required."
                            );
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_main
        );

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.rootScrollView),
                (v, insets) -> {
                    Insets systemBars =
                            insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );
                    return insets;
                }
        );

        initializeViews();

        bleManager =
                new BleManager(
                        this,
                        this
                );

        scanButton.setOnClickListener(
                view -> {

                    stopDemoMode();

                    if (!hasBluetoothPermissions()) {

                        requestBluetoothPermissions();

                    } else {

                        startBleScan();
                    }
                }
        );

        demoButton.setOnClickListener(
                view -> startDemoMode()
        );

        fallSimButton.setOnClickListener(
                view -> {
                    simulatedFallTriggered = true;
                    showToast("⚠️ Fall Alert Simulated!");
                    demoHandler.removeCallbacks(demoRunnable);
                    demoHandler.post(demoRunnable);
                }
        );

        disconnectButton.setOnClickListener(
                view -> {

                    stopDemoMode();

                    bleManager.disconnect();

                    connectionStatus.setText(
                            "Disconnected"
                    );
                }
        );
    }

    private void initializeViews() {

        connectionStatus =
                findViewById(
                        R.id.connectionStatus
                );

        heartRateValue =
                findViewById(
                        R.id.heartRateValue
                );

        spo2Value =
                findViewById(
                        R.id.spo2Value
                );

        temperatureValue =
                findViewById(
                        R.id.temperatureValue
                );

        humidityValue =
                findViewById(
                        R.id.humidityValue
                );

        activityValue =
                findViewById(
                        R.id.activityValue
                );

        fallValue =
                findViewById(
                        R.id.fallValue
                );

        scanButton =
                findViewById(
                        R.id.scanButton
                );

        demoButton =
                findViewById(
                        R.id.demoButton
                );

        fallSimButton =
                findViewById(
                        R.id.fallSimButton
                );

        disconnectButton =
                findViewById(
                        R.id.disconnectButton
                );
    }

    private void startDemoMode() {

        if (bleManager != null) {

            bleManager.stopScan();

            bleManager.disconnect();
        }

        isDemoMode = true;

        connectionStatus.setText(
                "Demo Mode Active (Simulated Sensors) ✓"
        );

        fallSimButton.setVisibility(View.VISIBLE);

        scanButton.setEnabled(true);

        showToast(
                "Demo mode active! Live sensor simulation running."
        );

        demoHandler.removeCallbacks(demoRunnable);

        demoHandler.post(demoRunnable);
    }

    private void stopDemoMode() {

        isDemoMode = false;

        demoHandler.removeCallbacks(demoRunnable);

        if (fallSimButton != null) {

            fallSimButton.setVisibility(View.GONE);
        }
    }

    private boolean hasBluetoothPermissions() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.S) {

            return ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
                    &&
                    ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED;

        } else {

            return ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestBluetoothPermissions() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.S) {

            permissionLauncher.launch(
                    new String[]{
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT
                    }
            );

        } else {

            permissionLauncher.launch(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION
                    }
            );
        }
    }

    private void startBleScan() {

        if (!bleManager.isBluetoothSupported()) {

            showToast(
                    "This phone does not support Bluetooth LE."
            );

            return;
        }

        if (!bleManager.isBluetoothEnabled()) {

            showToast(
                    "Please turn on Bluetooth."
            );

            Intent intent =
                    new Intent(
                            Settings.ACTION_BLUETOOTH_SETTINGS
                    );

            startActivity(intent);

            return;
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {

            LocationManager locationManager =
                    (LocationManager) getSystemService(LOCATION_SERVICE);

            boolean locationEnabled = locationManager != null &&
                    (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                            || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));

            if (!locationEnabled) {

                showToast(
                        "Please turn on Location (GPS) to scan for BLE devices."
                );

                startActivity(
                        new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                );

                return;
            }
        }

        connectionStatus.setText(
                "Scanning..."
        );

        scanButton.setEnabled(false);

        bleManager.startScan();
    }

    @Override
    public void onDeviceFound(
            BluetoothDevice device
    ) {

        runOnUiThread(() -> {

            connectionStatus.setText(
                    "Device found: "
                            + getSafeDeviceName(device)
            );

            bleManager.connect(device);
        });
    }

    private String getSafeDeviceName(
            BluetoothDevice device
    ) {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.S) {

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED) {

                return "HEALTH_COMPANION";
            }
        }

        String name = device.getName();

        if (name == null || name.trim().isEmpty()) {

            return "HEALTH_COMPANION";
        }

        return name;
    }

    @Override
    public void onConnecting() {

        runOnUiThread(() -> {

            connectionStatus.setText(
                    "Connecting..."
            );
        });
    }

    @Override
    public void onConnected() {

        runOnUiThread(() -> {

            connectionStatus.setText(
                    "Connected ✓"
            );

            scanButton.setEnabled(true);

            showToast(
                    "Health Companion connected."
            );
        });
    }

    @Override
    public void onDisconnected() {

        runOnUiThread(() -> {

            if (!isDemoMode) {

                connectionStatus.setText(
                        "Disconnected"
                );
            }

            scanButton.setEnabled(true);
        });
    }

    @Override
    public void onDataReceived(
            String data
    ) {

        runOnUiThread(() -> {

            try {

                HealthData healthData =
                        DataParser.parse(data);

                updateDashboard(
                        healthData
                );

            } catch (Exception e) {

                showToast(
                        "Received: " + data
                );
            }
        });
    }

    private void updateDashboard(
            HealthData data
    ) {

        heartRateValue.setText(
                data.getHeartRate()
                        + " BPM"
        );

        spo2Value.setText(
                data.getSpo2()
                        + " %"
        );

        temperatureValue.setText(
                String.format(
                        Locale.US,
                        "%.1f °C",
                        data.getTemperature()
                )
        );

        humidityValue.setText(
                String.format(
                        Locale.US,
                        "%.1f %%",
                        data.getHumidity()
                )
        );

        if (data.getActivity() == 1) {

            activityValue.setText(
                    "Active"
            );

        } else {

            activityValue.setText(
                    "Normal"
            );
        }

        if (data.getFall() == 1) {

            fallValue.setText(
                    "⚠ POSSIBLE FALL"
            );

        } else {

            fallValue.setText(
                    "No fall detected"
            );
        }
    }

    @Override
    public void onError(
            String message
    ) {

        runOnUiThread(() -> {

            if (!isDemoMode) {

                connectionStatus.setText(
                        "Status: " + message
                );
            }

            scanButton.setEnabled(true);

            showToast(message);
        });
    }

    private void showToast(
            String message
    ) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
        ).show();
    }

    @Override
    protected void onDestroy() {

        stopDemoMode();

        if (bleManager != null) {

            bleManager.stopScan();

            bleManager.closeGatt();
        }

        super.onDestroy();
    }
}
