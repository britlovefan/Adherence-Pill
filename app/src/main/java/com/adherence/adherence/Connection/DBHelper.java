package com.adherence.adherence.Connection;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;


public class DBHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "DeviceInfo";
    private static final String TABLE_NAME = "deviceTable";
    private static final String DEVICE_NAME = "device";
    private static final String USER_NAME = "username";
    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + "(" + USER_NAME + " STRING,"
                +  DEVICE_NAME + " STRING" + ")";
        db.execSQL(CREATE_TABLE);
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS" + TABLE_NAME);
        onCreate(db);
    }
    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public void addDevice(String username,String devicename){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(USER_NAME, username);
        values.put(DEVICE_NAME,devicename);
        db.insert(TABLE_NAME, null, values);
        db.close();
    }
    public ArrayList<String> getDevice(String username){
        ArrayList<String>  deviceList = new ArrayList<String>();
        String equalQuery ="SELECT * FROM " + TABLE_NAME + " WHERE "+USER_NAME+"='"+ username+"'";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(equalQuery, null);
        if (cursor.moveToFirst()) {
            do {
                String deviceName = cursor.getString(1);
                deviceList.add(deviceName);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return deviceList;
    }

}
