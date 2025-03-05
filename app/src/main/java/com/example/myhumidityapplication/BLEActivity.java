package com.example.myhumidityapplication;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public class BLEActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSION_BLUETOOTH = 100;
    private static final int REQUEST_PERMISSION_LOCATION = 101;
    private static final UUID HC05_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    Button buttonBack, ConnectDevice, disconnectDevice, startScanningButton, stopScanningButton;
    TextView DeviceText, displayMSG, tvBLEActivity;
    ListView deviceListView;
    BluetoothAdapter btAdapter;
    BluetoothDevice hc05Device;
    static BluetoothSocket bluetoothSocket; // Static to access from MainActivity
    OutputStream outputStream;
    InputStream inputStream;
    BluetoothManager btManager;
    ArrayAdapter<String> deviceListAdapter;
    List<BluetoothDevice> discoveredDevices = new ArrayList<>();
    private String finalMessage = "NO MSG RECEIVED";
    Handler handler = new Handler();

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && device.getName() != null) {
                    discoveredDevices.add(device);
                    deviceListAdapter.add(device.getName() + " - " + device.getAddress());
                    deviceListAdapter.notifyDataSetChanged();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_devices);


        buttonBack = findViewById(R.id.btnBack);
//        ConnectDevice = findViewById(R.id.connectDeviceBtn);
//        disconnectDevice = findViewById(R.id.disconnectDevice);
        startScanningButton = findViewById(R.id.StartScanButton);
//        stopScanningButton = findViewById(R.id.StopScanButton);
//        DeviceText = findViewById(R.id.txtDevice);
//        displayMSG = findViewById(R.id.textViewBLEActivity);
//        tvBLEActivity = findViewById(R.id.textViewBLEActivity);
        deviceListView = findViewById(R.id.listView);


        btAdapter = BluetoothAdapter.getDefaultAdapter();

        startScanningButton.setOnClickListener(v -> startScanning());

        buttonBack.setOnClickListener(v -> {
            Intent returnIntent = new Intent();
            returnIntent.putExtra("RECEIVED_MESSAGE", finalMessage);
            setResult(RESULT_OK, returnIntent);
            finish();
        });

        deviceListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedDevice = (String) parent.getItemAtPosition(position);
            Toast.makeText(this, "Selected: " + selectedDevice, Toast.LENGTH_SHORT).show(); // Debugging Toast

            int lastDashIndex = selectedDevice.lastIndexOf(" - ");
            if (lastDashIndex != -1) {
                String deviceAddress = selectedDevice.substring(lastDashIndex + 3).trim(); // Extract MAC address
                Toast.makeText(this, "Connecting to: " + deviceAddress, Toast.LENGTH_SHORT).show(); // Debugging Toast
                connectToDevice(deviceAddress);
            } else {
                Toast.makeText(this, "Invalid device format", Toast.LENGTH_SHORT).show();
            }
        });
    }

//    private void connectToDevice(String deviceAddress) {
//        BluetoothDevice device = btAdapter.getRemoteDevice(deviceAddress);
//        try {
//            bluetoothSocket = device.createRfcommSocketToServiceRecord(HC05_UUID);
//            bluetoothSocket.connect();
//            outputStream = bluetoothSocket.getOutputStream();
//            inputStream = bluetoothSocket.getInputStream();
//            Toast.makeText(this, "Connection to " + device.getName() + " - " + device.getAddress(), Toast.LENGTH_SHORT).show();
//            startListeningForData();
//        } catch (IOException e) {
//            e.printStackTrace();
//            Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show();
//        }
//    }

    private void connectToDevice(String deviceAddress) {
        BluetoothDevice device = btAdapter.getRemoteDevice(deviceAddress);
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(HC05_UUID);
            bluetoothSocket.connect();
            Toast.makeText(this, "Connected to " + device.getName(), Toast.LENGTH_SHORT).show();

            // Send BluetoothSocket to MainActivity
            Intent returnIntent = new Intent();
            returnIntent.putExtra("DEVICE_CONNECTED", true);
            setResult(RESULT_OK, returnIntent);
            finish();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show();
        }
    }



    private void startScanning() {
        if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        discoveredDevices.clear();
        deviceListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        deviceListView.setAdapter(deviceListAdapter);

        btAdapter.startDiscovery();
        registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
    }

    private BluetoothDevice getHC05Device() {
        for (BluetoothDevice device : btAdapter.getBondedDevices()) {
            if (device.getName().equals("HC-05")) {
                return device;
            }
        }
        return null;
    }

    private void startListeningForData() {
        new Thread(() -> {
            try {
                inputStream = bluetoothSocket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                while (bluetoothSocket.isConnected()) {
                    String receivedMessage = reader.readLine();
                    if (receivedMessage != null && !receivedMessage.isEmpty()) {
                        finalMessage = receivedMessage;
                        runOnUiThread(() -> {
                            displayMSG.setText("MSG: " + receivedMessage);
                            tvBLEActivity.setText("FINAL MESSAGE: " + receivedMessage);
                            Toast.makeText(BLEActivity.this, "Received: " + receivedMessage, Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static BluetoothSocket getBluetoothSocket() {
        return BLEActivity.bluetoothSocket;
    }
}

