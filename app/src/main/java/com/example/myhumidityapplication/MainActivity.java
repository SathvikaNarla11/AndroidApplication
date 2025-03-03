package com.example.myhumidityapplication;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
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
        updateUI(mode);

        autoBtn.setOnClickListener(v -> {
            databaseHelper.updateMode(1); // Set mode to Auto in database
            processReceivedData(databaseHelper.getHumidityData()); // Fetch latest data & update UI
        });

        manualBtn.setOnClickListener(v -> {
            databaseHelper.updateMode(0); // Set mode to Manual in database
            processReceivedData(databaseHelper.getHumidityData()); // Fetch latest data & update UI
        });




        // Fetch data from database
//        humidityData = databaseHelper.getHumidityData();
//        dataTextView.setText(humidityData);

        exeButton();
    }

    private void updateUI(int mode) {
        if (mode == 1) { // Automatic Mode
            autoBtn.setBackgroundColor(Color.GREEN);
            manualBtn.setBackgroundColor(Color.GRAY);

            fanStatusBtn.setEnabled(false);
            mistStatusBtn.setEnabled(false);
            ledStatusBtn.setEnabled(false);

            Toast.makeText(MainActivity.this, "AUTOMATIC MODE ON", Toast.LENGTH_SHORT).show();
        } else { // Manual Mode
            autoBtn.setBackgroundColor(Color.GRAY);
            manualBtn.setBackgroundColor(Color.GREEN);

            fanStatusBtn.setEnabled(true);
            mistStatusBtn.setEnabled(true);
            ledStatusBtn.setEnabled(true);

            Toast.makeText(MainActivity.this, "MANUAL MODE ON", Toast.LENGTH_SHORT).show();
        }
    }

    private void exeButton() {
        cntBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BLEActivity.class);
            startActivityForResult(intent, BLUETOOTH_REQUEST_CODE); // Expecting a result
        });
    }

    private void processReceivedData(String data) {
        if (data.startsWith("$MIST")) {
            String[] parts = data.split(",");
            if (parts.length == 7) {
                int mode = Integer.parseInt(parts[6].trim()); // Extract mode from last value
                boolean fanStatus = parts[3].trim().equals("1");
                boolean mistStatus = parts[4].trim().equals("1");
                boolean ledStatus = parts[5].trim().equals("1");

                runOnUiThread(() -> {
                    updateUI(mode); // Call updateUI to set button enable/disable
                    updateDeviceStatus(fanStatus, mistStatus, ledStatus); // Update button images and text
                });
            }
        }
    }

    private void updateDeviceStatus(boolean fanStatus, boolean mistStatus, boolean ledStatus) {
        fanImgStatus.setImageResource(fanStatus ? R.drawable.on_button : R.drawable.off_button);
        mistImgStatus.setImageResource(mistStatus ? R.drawable.on_button : R.drawable.off_button);
        ledImgStatus.setImageResource(ledStatus ? R.drawable.on_button : R.drawable.off_button);

        // Only update text and color in Manual Mode
        if (manualBtn.getCurrentTextColor() == Color.GREEN) {
            fanStatusBtn.setBackgroundColor(fanStatus ? Color.GREEN : Color.RED);
            fanStatusBtn.setText(fanStatus ? "ON" : "OFF");

            mistStatusBtn.setBackgroundColor(mistStatus ? Color.GREEN : Color.RED);
            mistStatusBtn.setText(mistStatus ? "ON" : "OFF");

            ledStatusBtn.setBackgroundColor(ledStatus ? Color.GREEN : Color.RED);
            ledStatusBtn.setText(ledStatus ? "ON" : "OFF");
        }
    }



//    private void processReceivedData(String data) {
//        if (data.startsWith("$MIST")) {
//            String[] parts = data.split(",");
//            if (parts.length == 7) {
//                String temperature = parts[1].trim();
//                String humidity = parts[2].trim();
//                boolean fanStatus = parts[3].trim().equals("1");
//                boolean mistStatus = parts[4].trim().equals("1");
//                boolean ledStatus = parts[5].trim().equals("1");
//                boolean isAutoMode = parts[6].trim().equals("1");
//
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Toast.makeText(MainActivity.this, "RUN", Toast.LENGTH_SHORT).show();
//
//                        tempValueTV.setText(temperature);
//                        humidityValueTV.setText(humidity);
//
//                        if (isAutoMode) {
//                            Toast.makeText(MainActivity.this, "AUTOMATIC MODE ON", Toast.LENGTH_SHORT).show();
//                            autoBtn.setBackgroundColor(Color.GREEN);
//                            manualBtn.setBackgroundColor(Color.GRAY);
//
//                            fanStatusBtn.setEnabled(false);
//                            mistStatusBtn.setEnabled(false);
//                            ledStatusBtn.setEnabled(false);
//
//                            fanImgStatus.setImageResource(fanStatus ? R.drawable.on_button: R.drawable.off_button );
//                            mistImgStatus.setImageResource(mistStatus ? R.drawable.on_button : R.drawable.off_button );
//                            ledImgStatus.setImageResource(ledStatus ? R.drawable.on_button : R.drawable.off_button );
//
//                        }
//                        else {
//                            Toast.makeText(MainActivity.this, "MANUAL MODE ON", Toast.LENGTH_SHORT).show();
//                            autoBtn.setBackgroundColor(Color.GRAY);
//                            manualBtn.setBackgroundColor(Color.GREEN);
//
//                            fanStatusBtn.setEnabled(true);
//                            mistStatusBtn.setEnabled(true);
//                            ledStatusBtn.setEnabled(true);
//
//                            fanStatusBtn.setBackgroundColor(fanStatus ? Color.GREEN : Color.RED);
//                            fanStatusBtn.setText(fanStatus ? "ON" : "OFF");
//                            fanImgStatus.setImageResource(fanStatus ? R.drawable.on_button: R.drawable.off_button );
//
//                            mistStatusBtn.setBackgroundColor(mistStatus ? Color.GREEN : Color.RED);
//                            mistStatusBtn.setText(mistStatus ? "ON" : "OFF");
//                            mistImgStatus.setImageResource(mistStatus ? R.drawable.on_button : R.drawable.off_button );
//
//                            ledStatusBtn.setBackgroundColor(ledStatus ? Color.GREEN : Color.RED);
//                            ledStatusBtn.setText(ledStatus ? "ON" : "OFF");
//                            ledImgStatus.setImageResource(ledStatus ? R.drawable.on_button : R.drawable.off_button );
//                        }
//                    }
//                });
//            }
//        }
//    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == BLUETOOTH_REQUEST_CODE && resultCode == RESULT_OK) {
            boolean isBluetoothOn = data.getBooleanExtra("BLUETOOTH_STATE", false);
            if (isBluetoothOn) {
                cntBtn.setBackgroundColor(Color.parseColor("#0288D1"));
                autoBtn.setEnabled(true);
                manualBtn.setEnabled(true);

                humidityData = databaseHelper.getHumidityData();
                dataTextView.setText(humidityData);

                processReceivedData(humidityData);
            } else {
                cntBtn.setBackgroundColor(Color.RED);
            }
        }
    }
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



