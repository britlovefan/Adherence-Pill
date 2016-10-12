package com.adherence.adherence;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

import com.parse.ParseObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * Created by caffery on 4/25/16.
 */
public class MyService extends Service {
    // Binder given to clients
    private final IBinder binder = new LocalBinder();
    // Registered callbacks
    private ServiceCallbacks serviceCallbacks;

    private final Random mGenerator = new Random();

    // Class used for the client Binder.
    public class LocalBinder extends Binder {
        MyService getService() {
            // Return this instance of MyService so clients can call public methods
            return MyService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setCallbacks(ServiceCallbacks callbacks) {
        serviceCallbacks = callbacks;
    }
    /** method for clients */
    public int getRandomNumber() {
        return mGenerator.nextInt(100);
    }

    @Override
    public void onCreate() {

    }

    //LG: CALL THE SERVICE
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(getApplicationContext(),"On start command",Toast.LENGTH_LONG).show();
        if (serviceCallbacks != null) {
//            Toast.makeText(getApplicationContext(),"In service",Toast.LENGTH_LONG).show();
            callservice();
//            Toast.makeText(getApplicationContext(),"Call service",Toast.LENGTH_LONG).show();
        }
        return 1;
    }
    //LG: within the background service, call the fuction of main active to connec the deivce
    public void callservice( ) {
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                serviceCallbacks.doSomething();
                Date now = new Date();
                String timestamp = new SimpleDateFormat("yyyy/MM/dd").format(now);
                ParseObject testObject = new ParseObject("TestXZ");
                testObject.put("TIME", timestamp);
                testObject.put("NAME", "Lipitor");
                testObject.saveEventually();
                Toast.makeText(getApplicationContext(), "Test", Toast.LENGTH_LONG).show();
            }
        }, 3500);                   //call the dosomething() in main activity
    }
}
