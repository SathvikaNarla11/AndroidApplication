package com.example.myhumidityapplication;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;


public class ScanBluetoothActivity extends AppCompatActivity
{

    Button buttonOn, buttonBack;
    BluetoothAdapter myBluetoothAdapter;
    // Initialize Bluetooth Adapter


    Intent btEnablingIntent;

    ActivityResultLauncher<Intent> enableBluetoothLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Toast.makeText(getApplicationContext(), "Bluetooth Enabled", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Bluetooth Enabling Cancelled", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_bluetooth);

        buttonOn = findViewById(R.id.btnOnBT);
        buttonBack = findViewById(R.id.btnBack);


        buttonBack.setOnClickListener(v -> {
            boolean isBluetoothOn = (myBluetoothAdapter != null && myBluetoothAdapter.isEnabled());

            // Send Bluetooth status back to MainActivity
            Intent returnIntent = new Intent();
            returnIntent.putExtra("BLUETOOTH_STATE", isBluetoothOn);
            setResult(RESULT_OK, returnIntent);
            finish();  // This closes the current activity and goes back to MainActivity
        });

        //Step 4 : Create the object of bluetooth adapter class
        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        btEnablingIntent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        bluetoothONMethod();
    }

    private void bluetoothONMethod() {
        buttonOn.setOnClickListener(view -> {
            if (myBluetoothAdapter == null) {
                Toast.makeText(getApplicationContext(), "Bluetooth not supported", Toast.LENGTH_LONG).show();
            } else {
                // Check if Bluetooth permission is granted (for Android 12+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                                != PackageManager.PERMISSION_GRANTED) {

                    // Request permission
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
                    return;
                }
                // If permission is granted, proceed with enabling Bluetooth
                if (!myBluetoothAdapter.isEnabled()) {
                    Toast.makeText(getApplicationContext(), "Requesting to enable Bluetooth", Toast.LENGTH_SHORT).show();
                    enableBluetoothLauncher.launch(btEnablingIntent);
                }
            }
        });
    }
}
