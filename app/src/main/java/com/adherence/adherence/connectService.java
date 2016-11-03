package com.adherence.adherence;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.zentri.zentri_ble_command.ZentriOSBLEManager;

import java.util.ArrayList;

/**
 * Created by Christina on 2016/10/16.
 */
public class connectService extends IntentService {
    private ZentriOSBLEService mService;
    private ServiceConnection mConnection;
    private boolean mBound = false;
    private ZentriOSBLEManager mZentriOSBLEManager;
    private ArrayList<String> mValidDeviceList;
    public connectService(){
        super("connectService");
    }
    @Override
    protected void onHandleIntent(Intent intent){
        //Task
        Log.v("Start","Background");
        mValidDeviceList = intent.getStringArrayListExtra("DEVICETOSERVICE");
        initServiceConnection();
        //mZentriOSBLEManager = new ZentriOSBLEManager();

        startService(new Intent(this, ZentriOSBLEService.class));
        //check whether the deviceList has been passed correctly
        if(mBound==false){
            Log.v("Service","Not start");
        }
        Log.v("list",mValidDeviceList.get(0));
        //Trying to connect all the device
        for(String s:mValidDeviceList) {
            mZentriOSBLEManager.connect(s);
        }
        Log.v("connect Service", "runs correctly!");
        stopService(new Intent(this, ZentriOSBLEService.class));
        Log.v("disconnect Service", "disconnect correctly!");
    }

    private void initServiceConnection()
    {
        mConnection = new ServiceConnection()
        {
            @Override
            public void onServiceConnected(ComponentName className, IBinder service)
            {
                ZentriOSBLEService.LocalBinder binder = (ZentriOSBLEService.LocalBinder) service;
                mService = binder.getService();
                mBound = true;
                mZentriOSBLEManager = mService.getManager();
                if(!mZentriOSBLEManager.isInitialised())
                {
                    mService.initTruconnectManager();//try again
                    if (!mZentriOSBLEManager.isInitialised())
                    {
                        Log.v("Manager","IS not initilized");
                    }
                }
            }
            @Override
            public void onServiceDisconnected(ComponentName arg0)
            {
                mBound = false;
            }
        };
    }

}