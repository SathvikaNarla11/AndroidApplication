package com.example.myhumidityapplication;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "HumidityData.db"; // Database Name
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "HumidityTable"; // Table Name

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create table query
        String createTableQuery = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                "mist TEXT, " +
                "temperature REAL, " +
                "humidity REAL, " +
                "fan_status INTEGER, " +
                "mist_status INTEGER, " +
                "led_status INTEGER, " +
                "mode INTEGER)";
        db.execSQL(createTableQuery);

        // Insert default data (Only runs once)
        insertDefaultData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // Insert Sample Data
    private void insertDefaultData(SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put("mist", "$MIST");
        values.put("temperature", 40.5);
        values.put("humidity", 101.5);
        values.put("fan_status", 1);
        values.put("mist_status", 1);
        values.put("led_status", 0);
        values.put("mode", 0);
        db.insert(TABLE_NAME, null, values);
    }

    // Retrieve Data
    public String getHumidityData() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " LIMIT 1", null);
        String dataString = "No Data Found";

        if (cursor.moveToFirst()) {
            String mist = cursor.getString(0);
            double temperature = cursor.getDouble(1);
            double humidity = cursor.getDouble(2);
            int fanStatus = cursor.getInt(3);
            int mistStatus = cursor.getInt(4);
            int ledStatus = cursor.getInt(5);
            int mode = cursor.getInt(6);

            // Format Data
//            dataString = mist + ", " + temperature + "°C, " + humidity + " Rh, " +
//                    "Fan: " + fanStatus + ", Mist: " + mistStatus +
//                    ", LED: " + ledStatus + ", Mode: " + mode;
            dataString = mist+","+temperature+","+humidity+","+fanStatus+","+mistStatus+","+ledStatus+","+mode;
        }
        cursor.close();
        db.close();
        return dataString;
    }

    // Fetch Mode from Database
    public int getMode() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT mode FROM " + TABLE_NAME + " LIMIT 1", null);
        int mode = 0; // Default to manual

        if (cursor.moveToFirst()) {
            mode = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return mode;
    }

    // Update Mode in Database
    public void updateMode(int newMode) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("mode", newMode);
        db.update(TABLE_NAME, values, null, null);
        db.close();
    }
}
