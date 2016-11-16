package com.adherence.adherence;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;

/**
 * Created by Christina on 2016/10/16.
 */
public class myAlarmReceiver extends BroadcastReceiver {
    public static final int REQUEST_CODE = 12345;
    public static final String ACTION = "com.codepath.example.servicesdemo.alarm";
    // Triggered by the Alarm periodically (starts the service to run task)
    @Override
    public void onReceive(Context context, Intent intent) {
        ArrayList<String> list = (ArrayList<String>) intent.getStringArrayListExtra("DEVICE_NAME");
        Intent ZentriService = new Intent(context, ZentriOSBLEService.class);
        ZentriService.putStringArrayListExtra("DEVICETOSERVICE",list);
        context.startService(ZentriService);
    }
}