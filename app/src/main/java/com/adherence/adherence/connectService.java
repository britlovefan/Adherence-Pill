package com.adherence.adherence;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.zentri.zentri_ble_command.ZentriOSBLEManager;

/**
 * Created by Christina on 2016/10/16.
 */
public class connectService extends IntentService {
    private ZentriOSBLEService mService;
    private ServiceConnection mConnection;
    private boolean mBound = false;
    private ZentriOSBLEManager mZentriOSBLEManager;
    public connectService(){
        super("connectService");
    }
    @Override
    protected void onHandleIntent(Intent intent){
        //Task
        startService(new Intent(this, ZentriOSBLEService.class));
        initServiceConnection();

        Log.v("connect Service", "runs correctly!");
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
