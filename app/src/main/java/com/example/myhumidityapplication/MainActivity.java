package com.example.myhumidityapplication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import kotlinx.coroutines.channels.ChannelSegment;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE = 1;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private static final int REQUEST_BLE_ACTIVITY = 1;
    private volatile String latestMessage = "";

    Button cntBtn, autoBtn, manualBtn, fanStatusBtn, mistStatusBtn, ledStatusBtn;
    ImageView fanImgStatus, mistImgStatus, ledImgStatus;
    TextView dataTextView, tempValueTV, humidityValueTV, displayMSG, timeStampDisplay;
    DatabaseHelper databaseHelper;
    BluetoothAdapter bluetoothAdapter;
    Toolbar toolbar;
    private OutputStream bluetoothOutputStream;

    boolean fan, led, mist;
    private boolean autoMode = false; // Default to Manual Mode




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cntBtn = findViewById(R.id.btnConnect);
        displayMSG = findViewById(R.id.dataTextView);
        autoBtn = findViewById(R.id.btnAutomatic);
        manualBtn = findViewById(R.id.btnManual);
        fanStatusBtn = findViewById(R.id.btnFan);
        mistStatusBtn = findViewById(R.id.btnMist);
        ledStatusBtn = findViewById(R.id.btnLed);
        tempValueTV = findViewById(R.id.tempValue);
        humidityValueTV = findViewById(R.id.humidityValue);
        fanImgStatus = findViewById(R.id.imageViewFan);
        mistImgStatus = findViewById(R.id.imageViewMist);
        ledImgStatus = findViewById(R.id.imageViewLed);
        timeStampDisplay = findViewById(R.id.textViewTimestamp);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        autoBtn.setEnabled(false);
        manualBtn.setEnabled(false);
        fanStatusBtn.setEnabled(false);
        mistStatusBtn.setEnabled(false);
        ledStatusBtn.setEnabled(false);


        // Open BLEActivity for scanning & connection
        cntBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BLEActivity.class);
            startActivityForResult(intent, REQUEST_BLE_ACTIVITY);
        });

        // Handle Button Clicks for Switching Modes
        autoBtn.setOnClickListener(v -> {
            sendCommandToDevice("1"); // Send command to switch to Auto Mode

        });

        manualBtn.setOnClickListener(v -> {
            sendCommandToDevice("2"); // Send command to switch to Manual Mode
        });

        fanStatusBtn.setOnClickListener(v -> {
            String status = fanStatusBtn.getText().toString();
            boolean isFanOn = status.equals("ON");
            sendCommandToDevice(isFanOn ? "4" : "3");
            Toast.makeText(getApplicationContext(), "Button Clicked: " + status, Toast.LENGTH_SHORT).show();
        });

        ledStatusBtn.setOnClickListener(v -> {
            String status = ledStatusBtn.getText().toString();
            if(status.equals("ON"))
            {
                sendCommandToDevice("8");
            }
            else{
                sendCommandToDevice("7");
            }
            Toast.makeText(getApplicationContext(), "Button Clicked: " + status, Toast.LENGTH_SHORT).show();
        });

        mistStatusBtn.setOnClickListener(v -> {
            String status = mistStatusBtn.getText().toString();
            if(status.equals("ON"))
            {
                sendCommandToDevice("6");
            }
            else{
                sendCommandToDevice("5");
            }
            Toast.makeText(getApplicationContext(), "Button Clicked: " + status, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_BLE_ACTIVITY && resultCode == RESULT_OK) {
            boolean deviceConnected = data.getBooleanExtra("DEVICE_CONNECTED", false);
            if (deviceConnected) {

                bluetoothSocket = BLEActivity.bluetoothSocket; // Get Bluetooth socket from BLEActivity
                initializeBluetoothOutputStream();

                //Enable buttons
                autoBtn.setEnabled(true);
                manualBtn.setEnabled(true);

                startListeningForData();
            }
        }
    }

    private void startListeningForData() {
        new Thread(() -> {
            try {
                InputStream inputStream = BLEActivity.getBluetoothSocket().getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                while (true) {  // Keep listening
                    String receivedMessage = reader.readLine();
                    if (receivedMessage != null && !receivedMessage.isEmpty()) {
                        latestMessage = receivedMessage;
                        runOnUiThread(() -> {
                            processReceivedMessage(receivedMessage);
//                            displayMSG.setText("MSG: " + receivedMessage);
                        });
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
    private void processReceivedMessage(String message) {
        displayMSG.setText("MSG in processRec: " + message); // Update UI
        // Get the current time
        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

        // Extract Auto/Manual Mode (Last Value)
        String[] dataParts = message.split(",");
        if (dataParts.length == 7) {

            String temperature = dataParts[1].trim();
            String humidity = dataParts[2].trim();
            boolean fanStatus = dataParts[3].trim().equals("1");
            boolean mistStatus = dataParts[4].trim().equals("1");
            boolean ledStatus = dataParts[5].trim().equals("1");
            boolean isAutoMode = dataParts[6].trim().equals("1");

            fan = fanStatus;
            led = ledStatus;
            mist = mistStatus;

            timeStampDisplay.setText(currentTime);
            tempValueTV.setText(temperature);
            humidityValueTV.setText(humidity);

            // Handle Mode Switching
            if (isAutoMode) {
                setAutoMode();
            } else {
                setManualMode(fanStatus, mistStatus, ledStatus);
            }
        }
    }

    // Function to enable Auto Mode
    private void setAutoMode() {
        runOnUiThread(() -> {
            autoBtn.setBackgroundColor(Color.GREEN);
            manualBtn.setBackgroundColor(Color.GRAY);

            // Disable fan, mist, and led buttons
            fanStatusBtn.setEnabled(false);
            mistStatusBtn.setEnabled(false);
            ledStatusBtn.setEnabled(false);

            updateButtonStateAutomatic(fanStatusBtn);
            updateButtonStateAutomatic(mistStatusBtn);
            updateButtonStateAutomatic(ledStatusBtn);
        });
    }

    // Function to enable Manual Mode
    private void setManualMode(boolean fanStatus, boolean mistStatus, boolean ledStatus) {
        runOnUiThread(() -> {
        autoBtn.setBackgroundColor(Color.GRAY);
        manualBtn.setBackgroundColor(Color.GREEN);

        // Enable fan, mist, and led buttons
        fanStatusBtn.setEnabled(true);
        mistStatusBtn.setEnabled(true);
        ledStatusBtn.setEnabled(true);

        // Update button states based on received values
            updateButtonStateManual(fanStatusBtn, fanStatus);
            updateButtonStateManual(mistStatusBtn, mistStatus);
            updateButtonStateManual(ledStatusBtn, ledStatus);



        });
    }

    // Function to update button text and color
    private void updateButtonStateManual(Button button, boolean isOn) {
        if (isOn) {
            button.setText("ON");
            button.setBackgroundColor(Color.GREEN);
        } else {
            button.setText("OFF");
            button.setBackgroundColor(Color.RED);
        }
    }

    private void updateButtonStateAutomatic(Button button) {

            button.setBackgroundColor(Color.GRAY);

    }

    private void sendCommandToDevice(String command) {
        if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
            if (bluetoothOutputStream != null) {
                try {
                    bluetoothOutputStream.write(command.getBytes());
                    bluetoothOutputStream.flush();
                    Toast.makeText(this, "Sent: " + command, Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Toast.makeText(this, "Send Failed", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(this, "OutputStream is NULL!", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Bluetooth Not Connected!", Toast.LENGTH_SHORT).show();
        }
    }


    // Function to send command to Bluetooth device
//    private void sendCommandToDevice(String command) {
//        if (bluetoothSocket != null) {
//            Toast.makeText(this, "NOT NULL", Toast.LENGTH_SHORT).show();
//            if (bluetoothOutputStream != null) {
//                try {
//                    Toast.makeText(this, "UPDATED IN PACKET", Toast.LENGTH_SHORT).show();
//                    bluetoothOutputStream.write((command).getBytes());
//                    bluetoothOutputStream.flush();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//            else {
//                Toast.makeText(this, "NOT UPDATED IN PACKET", Toast.LENGTH_SHORT).show();
//            }
//        }
//        else {
//            Toast.makeText(this, "NULL", Toast.LENGTH_SHORT).show();
//        }
//    }

    private void initializeBluetoothOutputStream() {
        if (bluetoothSocket != null) {
            try {
                bluetoothOutputStream = bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}

