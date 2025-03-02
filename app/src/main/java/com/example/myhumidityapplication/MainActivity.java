package com.example.myhumidityapplication;

import android.bluetooth.BluetoothAdapter;
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

public class MainActivity extends AppCompatActivity {

    Button cntBtn, autoBtn, manualBtn, fanStatusBtn, mistStatusBtn, ledStatusBtn;
    ImageView fanImgStatus, mistImgStatus, ledImgStatus;
    TextView dataTextView, tempValueTV, humidityValueTV;
    DatabaseHelper databaseHelper;
    BluetoothAdapter bluetoothAdapter;
    BroadcastReceiver bluetoothReceiver;
    Toolbar toolbar;
    private static final int BLUETOOTH_REQUEST_CODE = 1;

    private static final int REQUEST_CODE = 1;

    String humidityData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        cntBtn = findViewById(R.id.btnConnect);
        dataTextView = findViewById(R.id.dataTextView);
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

        autoBtn.setEnabled(false);
        manualBtn.setEnabled(false);
        fanStatusBtn.setEnabled(false);
        mistStatusBtn.setEnabled(false);
        ledStatusBtn.setEnabled(false);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        databaseHelper = new DatabaseHelper(this);
        int mode = databaseHelper.getMode();
        boolean fanStatusDB = databaseHelper.getFanStatus();
        boolean mistStatusDB = databaseHelper.getMistStatus();
        boolean ledStatusDB = databaseHelper.getLedStatus();
        updateUI(mode, fanStatusDB, mistStatusDB, ledStatusDB);

        autoBtn.setOnClickListener(v -> {
            databaseHelper.updateMode(1); // Set mode to Auto in database
            processReceivedData(databaseHelper.getHumidityData()); // Fetch latest data & update UI
        });

        manualBtn.setOnClickListener(v -> {
            databaseHelper.updateMode(0); // Set mode to Manual in database
            processReceivedData(databaseHelper.getHumidityData()); // Fetch latest data & update UI
        });

        fanStatusBtn.setOnClickListener(v -> {
            boolean currentFanStatus = databaseHelper.getFanStatus(); // Get latest status
            databaseHelper.updateFanStatus(currentFanStatus ? 0 : 1); // Toggle and update
            processReceivedData(databaseHelper.getHumidityData()); // Refresh UI
        });

        exeButton();
    }

    private void updateUI(int mode, boolean fanStatus, boolean mistStatus, boolean ledStatus) {
        if (mode == 1) { // Automatic Mode
            autoBtn.setBackgroundColor(Color.GREEN);
            manualBtn.setBackgroundColor(Color.GRAY);

            fanStatusBtn.setEnabled(false);
            mistStatusBtn.setEnabled(false);
            ledStatusBtn.setEnabled(false);

            fanImgStatus.setImageResource(fanStatus ? R.drawable.on_button: R.drawable.off_button );
            mistImgStatus.setImageResource(mistStatus ? R.drawable.on_button : R.drawable.off_button );
            ledImgStatus.setImageResource(ledStatus ? R.drawable.on_button : R.drawable.off_button );

            Toast.makeText(MainActivity.this, "AUTOMATIC MODE ON", Toast.LENGTH_SHORT).show();

        } else { // Manual Mode
            autoBtn.setBackgroundColor(Color.GRAY);
            manualBtn.setBackgroundColor(Color.GREEN);

            fanStatusBtn.setEnabled(true);
            mistStatusBtn.setEnabled(true);
            ledStatusBtn.setEnabled(true);

            fanStatusBtn.setBackgroundColor(fanStatus ? Color.GREEN : Color.RED);
            fanStatusBtn.setText(fanStatus ? "ON" : "OFF");
            fanImgStatus.setImageResource(fanStatus ? R.drawable.on_button: R.drawable.off_button );
//
            mistStatusBtn.setBackgroundColor(mistStatus ? Color.GREEN : Color.RED);
            mistStatusBtn.setText(mistStatus ? "ON" : "OFF");
            mistImgStatus.setImageResource(mistStatus ? R.drawable.on_button : R.drawable.off_button );
//
            ledStatusBtn.setBackgroundColor(ledStatus ? Color.GREEN : Color.RED);
            ledStatusBtn.setText(ledStatus ? "ON" : "OFF");
            ledImgStatus.setImageResource(ledStatus ? R.drawable.on_button : R.drawable.off_button );

            Toast.makeText(MainActivity.this, "MANUAL MODE ON", Toast.LENGTH_SHORT).show();

        }
    }

    private void exeButton() {
        cntBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BLEActivity.class);
            startActivityForResult(intent, REQUEST_CODE); // Expecting a result
        });
    }

    private void processReceivedData(String data) {
        if (data.startsWith("$MIST")) {
            String[] parts = data.split(",");
            if (parts.length == 7) {

                String temperature = parts[1].trim();
                String humidity = parts[2].trim();
                boolean fanStatus = parts[3].trim().equals("1");
                boolean mistStatus = parts[4].trim().equals("1");
                boolean ledStatus = parts[5].trim().equals("1");
//                boolean isAutoMode = parts[6].trim().equals("1");

                int mode = Integer.parseInt(parts[6].trim()); // Extract mode from last value

                runOnUiThread(() -> {
                    updateUI(mode, fanStatus, mistStatus, ledStatus); // Call updateUI to set button enable/disable
                });
            }
        }
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == BLUETOOTH_REQUEST_CODE && resultCode == RESULT_OK) {
//            boolean isBluetoothConnected = data.getBooleanExtra("BLUETOOTH_STATE", false);
//            if (isBluetoothConnected) {
//                Toast.makeText(this, "Bluetooth is connected!", Toast.LENGTH_SHORT).show();
//            } else {
//                Toast.makeText(this, "Bluetooth is not connected!", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    Log.d("MainActivity", "onActivityResult Triggered");

    if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
        Log.d("MainActivity", "Valid requestCode & resultCode");
        if (data != null && data.hasExtra("RECEIVED_MESSAGE")) {
            String receivedMessage = data.getStringExtra("RECEIVED_MESSAGE");

            if (receivedMessage == null || receivedMessage.isEmpty()) {
                Log.e("MainActivity", "Received message is NULL or EMPTY");
            } else {
                Log.d("MainActivity", "Received Message: " + receivedMessage);
                dataTextView.setText("haha " + receivedMessage); // Update TextView
                Toast.makeText(this, "Message from BLE: " + receivedMessage, Toast.LENGTH_SHORT).show();
            }
        }
        else {
            Log.e("MainActivity", "No extra data found in intent");
        }
    }
    else {
        Log.e("MainActivity", "Invalid requestCode or resultCode");
    }
}


//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if (requestCode == BLUETOOTH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
//            boolean isBluetoothConnected = data.getBooleanExtra("BLUETOOTH_STATE", false);
//            if (data.hasExtra("RECEIVED_MESSAGE")) {
//                String receivedMessage = data.getStringExtra("RECEIVED_MESSAGE");
//                // Update TextView only when a new message is received
//                if (receivedMessage != null && !receivedMessage.isEmpty()) {
//                    dataTextView.setText("haha " + receivedMessage);
//                }
//                Toast.makeText(this, "Message from BLE: " + receivedMessage, Toast.LENGTH_SHORT).show();
//            } else {
//                Toast.makeText(this, isBluetoothConnected ? "Bluetooth is connected!" : "Bluetooth is not connected!", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }





}




//         Initialize Bluetooth Receiver
//        bluetoothReceiver = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
//                if (state == BluetoothAdapter.STATE_ON) {
//                    cntBtn.setBackgroundColor(Color.GREEN);
//                    Toast.makeText(getApplicationContext(), "Bluetooth is ON. btnAutomatic Enabled!", Toast.LENGTH_SHORT).show();
//                } else if (state == BluetoothAdapter.STATE_OFF) {
//                    cntBtn.setBackgroundColor(Color.RED);
//                Toast.makeText(getApplicationContext(), "Turn ON Bluetooth to enable automatic mode!", Toast.LENGTH_SHORT).show();
//            }
//        }
//    };
//
//    // Register the Receiver
//    IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
//    registerReceiver(bluetoothReceiver, filter);



