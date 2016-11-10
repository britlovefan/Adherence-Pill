package com.adherence.adherence;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.adherence.adherence.Connection.DBHelper;
import com.zentri.zentri_ble_command.BLECallbacks;
import com.zentri.zentri_ble_command.Command;
import com.zentri.zentri_ble_command.ErrorCode;
import com.zentri.zentri_ble_command.Result;
import com.zentri.zentri_ble_command.ZentriOSBLEManager;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class ZentriOSBLEService extends Service implements Serializable
{
    public static final String ACTION_SCAN_RESULT = "ACTION_SCAN_RESULT";
    public static final String ACTION_CONNECTED = "ACTION_CONNECTED";
    public static final String ACTION_DISCONNECTED = "ACTION_DISCONNECTED";
    public static final String ACTION_MODE_WRITE = "ACTION_MODE_WRITE";
    public static final String ACTION_MODE_READ = "ACTION_MODE_READ";
    public static final String ACTION_STRING_DATA_WRITE = "ACTION_STRING_DATA_WRITE";
    public static final String ACTION_BINARY_DATA_WRITE = "ACTION_BINARY_DATA_WRITE";
    public static final String ACTION_STRING_DATA_READ = "ACTION_STRING_DATA_READ";
    public static final String ACTION_BINARY_DATA_READ = "ACTION_BINARY_DATA_READ";
    public static final String ACTION_COMMAND_SENT = "ACTION_COMMAND_SENT";
    public static final String ACTION_COMMAND_RESULT = "ACTION_COMMAND_RESULT";
    public static final String ACTION_VERSION_READ = "ACTION_VERSION_READ";
    public static final String ACTION_ERROR = "ACTION_ERROR";

    public static final String ACTION_OTA_INIT = "ACTION_OTA_INIT";
    public static final String ACTION_OTA_CHECK = "ACTION_OTA_CHECK";
    public static final String ACTION_OTA_ABORT = "ACTION_OTA_ABORT";
    public static final String ACTION_OTA_START = "ACTION_OTA_START";
    public static final String ACTION_OTA_DATA_SENT = "ACTION_OTA_DATA_SENT";
    public static final String ACTION_OTA_DONE = "ACTION_OTA_DONE";
    public static final String ACTION_OTA_ERROR = "ACTION_OTA_ERROR";

    public static final String EXTRA_MODE = "EXTRA_MODE";
    public static final String EXTRA_DATA = "EXTRA_DATA";
    public static final String EXTRA_ID = "EXTRA_ID";
    public static final String EXTRA_COMMAND = "EXTRA_COMMAND";
    public static final String EXTRA_RESPONSE_CODE = "EXTRA_RESPONSE_CODE";
    public static final String EXTRA_ERROR = "EXTRA_ERROR";

    public static final String EXTRA_VERSION = "EXTRA_VERSION";
    public static final String EXTRA_NAME = "EXTRA_NAME";
    public static final String EXTRA_IS_UP_TO_DATE = "EXTRA_IS_UP_TO_DATE";

    public static final int SERVICES_NONE = 0;
    public static final int SERVICES_TRUCONNECT_ONLY = 1;
    public static final int SERVICES_OTA_ONLY = 2;
    public static final int SERVICES_BOTH = 3;

    private static final boolean DISABLE_TX_NOTIFY = true;

    private final String TAG = "ZentriOSBLEService";

    private final boolean TX_NOTIFY_DISABLE = true;

    private final int mStartMode = START_NOT_STICKY;
    private final IBinder mBinder = new LocalBinder();
    boolean mAllowRebind = true;
    private ZentriOSBLEManager mZentriOSBLEManager;

    private BLECallbacks mCallbacks;
    private LocalBroadcastManager mBroadcastManager;
    private ArrayList<String> mDeviceList;

    public DBHelper db;
    public Result dataResult = null;
    private String response;

    public class LocalBinder extends Binder
    {
        ZentriOSBLEService getService()
        {
            // Return this instance of LocalService so clients can call public methods
            return ZentriOSBLEService.this;
        }
    }

    @Override
    public void onCreate()
    {
        // The service is being created
        Log.d(TAG, "Creating service");
        mZentriOSBLEManager = new ZentriOSBLEManager();
        mBroadcastManager = LocalBroadcastManager.getInstance(this);
        initCallbacks();
        initTruconnectManager();

    }
    //connect to the device in the list
    public void connect(ArrayList<String>list){
        for(String deviveName:list){
            mZentriOSBLEManager.connect(deviveName);
            //make sure that the device is actually connected before set time
            Thread t = new Thread() {
                @Override
                public void run() {
                    try {
                        //check if connected!
                        while (!mZentriOSBLEManager.isConnected()) {
                            Thread.sleep(500);
                        }
                        setTime();
                        mZentriOSBLEManager.setReceiveMode(com.zentri.zentri_ble.BLECallbacks.ReceiveMode.STRING);
                        mZentriOSBLEManager.writeData("*gn#");
                        if(response=="N"){
                            Log.v("Bottles correctly","But");
                        }
                    } catch (Exception e) {
                    }
                }
            };
            t.start();

            //ask for updates
            /*
            mZentriOSBLEManager.writeData("*gn#");
            if(dataResult!=null) {
                String text = dataResult.getData();
                if(text=="N"){
                    mZentriOSBLEManager.disconnect(true);
                    Log.v("No new data","disconnect device");
                }
            }
            else{
                Log.v("ERROR","no response from bottles");
                break;
            }*/
        }
    }
    //send and set the current time to the bottle every time we connect to the bottle
    public void setTime(){
        Calendar c = Calendar.getInstance();
        //004814107032016
        //14:48:00; March 7, 2016
        SimpleDateFormat sdf = new SimpleDateFormat("ssmmHHddmmyyyy");
        String strDate = sdf.format(c.getTime());
        String timeCommand = "*st" + strDate + "#";
        mZentriOSBLEManager.writeData(timeCommand);
        Log.v("Set Time correctly!",strDate);
        /*
        timeNow = Calendar.getInstance();
        int second = timeNow.get(Calendar.SECOND);
        int minute = timeNow.get(Calendar.MINUTE);
        int hour = timeNow.get(Calendar.HOUR_OF_DAY);
        int dayOfWeek = timeNow.get(Calendar.DAY_OF_WEEK);
        int dayOfMonth = timeNow.get(Calendar.DAY_OF_MONTH);
        int month = timeNow.get(Calendar.MONTH);
        int year = timeNow.get(Calendar.YEAR);
       //String rtc_data = Integer.toString(second) + Integer.toString(minute) + Integer.toString(hour) + Integer.toString(dayOfWeek) + Integer.toString(dayOfMonth) + Integer.toString(month) + Integer.toString(year);
        String rtc_data = String.format("%02d%02d%02d%d%02d%02d%04d", second, minute, hour, dayOfWeek, dayOfMonth, month + 1, year);
         dataToSend = "*T" + rtc_data + "#";
         */
    }
    private void startDeviceInfoActivity() {
        /*
        mZentriOSBLEManager.setMode(ZentriOSBLEManager.MODE_COMMAND_REMOTE);
        mZentriOSBLEManager.setSystemCommandMode(CommandMode.MACHINE);*/
        setTime();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        //trying to receive the arraylist

        if(intent != null){
            /*
            db = new DBHelper(this);
            ArrayList<String> results = db.getDevice("christina");
            connect(results);
            */
            mDeviceList = intent.getStringArrayListExtra("DEVICETOSERVICE");
            if(mDeviceList!=null) connect(mDeviceList);
            if(mZentriOSBLEManager.isConnected()){
                Log.v("Device is connected","correctly");
            }
        }
        // Try to receive the list  from the database
        // The service is starting, due to a call to startService()
        return mStartMode;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        // A client is binding to the service with bindService()
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        // All clients have unbound with unbindService()
        return mAllowRebind;
    }

    @Override
    public void onRebind(Intent intent)
    {
        // A client is binding to the service with bindService(),
        // after onUnbind() has already been called
    }

    @Override
    public void onDestroy()
    {
        // The service is no longer used and is being destroyed
        Log.d(TAG, "Destroying service");
        if (mZentriOSBLEManager != null)
        {
            mZentriOSBLEManager.stopScan();
            mZentriOSBLEManager.disconnect(DISABLE_TX_NOTIFY);//ensure all connections are terminated
            mZentriOSBLEManager.deinit();
        }
    }

    public ZentriOSBLEManager getManager()
    {
        return mZentriOSBLEManager;
    }

    public boolean initTruconnectManager()
    {
        return mZentriOSBLEManager.init(ZentriOSBLEService.this, mCallbacks);
    }

    private void initCallbacks()
    {
        mCallbacks = new BLECallbacks()
        {
            @Override
            public void onScanResult(String deviceName, String address)
            {
                Log.d(TAG, "onScanResult");
                Intent intent = new Intent(ACTION_SCAN_RESULT);
                intent.putExtra(EXTRA_DATA, deviceName);
                mBroadcastManager.sendBroadcast(intent);
            }

            @Override
            public void onFirmwareVersionRead(String deviceName, String version)
            {
                Log.d(TAG, "onFirmwareVersionRead: " + deviceName + " version: " + version);
            }

            @Override
            public void onConnected(String deviceName, int services)
            {
                Log.d(TAG, deviceName+" onConnected");
                //startDeviceInfoActivity();
                Intent intent = new Intent(ACTION_CONNECTED);
                intent.putExtra(EXTRA_NAME, deviceName);
                intent.putExtra(EXTRA_DATA, services);
                mBroadcastManager.sendBroadcast(intent);
            }

            @Override
            public void onDisconnected()
            {
                Log.d(TAG, "onDisconnected");
                Intent intent = new Intent(ACTION_DISCONNECTED);
                mBroadcastManager.sendBroadcast(intent);
            }

            @Override
            public void onModeWritten(int mode)
            {
                Log.d(TAG, "onModeWritten");
                Intent intent = new Intent(ACTION_MODE_WRITE);
                intent.putExtra(EXTRA_MODE, mode);
                mBroadcastManager.sendBroadcast(intent);
            }

            @Override
            public void onModeRead(int mode)
            {
                Log.d(TAG, "onModeRead");
                Intent intent = new Intent(ACTION_MODE_READ);
                intent.putExtra(EXTRA_MODE, mode);
                mBroadcastManager.sendBroadcast(intent);
            }

            @Override
            public void onStringDataWritten(String data)
            {
                Log.d(TAG, "onStringDataWritten - " + data);
                Intent intent = new Intent(ACTION_STRING_DATA_WRITE);
                intent.putExtra(EXTRA_DATA, data);
                mBroadcastManager.sendBroadcast(intent);
            }

            @Override
            public void onBinaryDataWritten(byte[] data)
            {
                Log.d(TAG, "onBinaryDataWritten - Wrote " + data.length + " bytes");
                Intent intent = new Intent(ACTION_BINARY_DATA_WRITE);
                intent.putExtra(EXTRA_DATA, data);
                mBroadcastManager.sendBroadcast(intent);
            }

            @Override
            public void onStringDataRead(String data)
            {
                Log.d(TAG, "onDataRead - " + data);
                //add
                response = data;
                Intent intent = new Intent(ACTION_STRING_DATA_READ);
                intent.putExtra(EXTRA_DATA, data);
                mBroadcastManager.sendBroadcast(intent);
            }

            @Override
            public void onBinaryDataRead(byte[] data)
            {
                Log.d(TAG, "onBinaryDataRead");

                Intent intent = new Intent(ACTION_BINARY_DATA_READ);
                intent.putExtra(EXTRA_DATA, data);
                mBroadcastManager.sendBroadcast(intent);
            }

            @Override
            public void onCommandSent(int ID, Command command)
            {
                Log.d(TAG, "onCommandSent");
                Intent intent = new Intent(ACTION_COMMAND_SENT);
                intent.putExtra(EXTRA_ID, ID);
                intent.putExtra(EXTRA_COMMAND, command);
                mBroadcastManager.sendBroadcast(intent);
            }

            @Override
            public void onCommandResult(int ID, Command command, Result result)
            {
                Log.d(TAG, "onCommandResult");
                Intent intent = new Intent(ACTION_COMMAND_RESULT);
                intent.putExtra(EXTRA_COMMAND, command);
                //added code
                if (result != null)
                {
                    intent.putExtra(EXTRA_ID, ID);
                    intent.putExtra(EXTRA_RESPONSE_CODE, result.getResponseCode());
                    intent.putExtra(EXTRA_DATA, result.getData());
                    dataResult = result;
                }
                mBroadcastManager.sendBroadcast(intent);
            }

            @Override
            public void onError(ErrorCode error)
            {
                Intent intent = new Intent(ACTION_ERROR);
                intent.putExtra(EXTRA_ERROR, error);
                mBroadcastManager.sendBroadcast(intent);
                Log.d(TAG, "onError - " + error);
            }
        };
    }

    public static int getMode(Intent intent)
    {
        return intent.getIntExtra(EXTRA_MODE, 0);
    }

    public static String getData(Intent intent)
    {
        return intent.getStringExtra(EXTRA_DATA);
    }

    public static byte[] getBinaryData(Intent intent)
    {
        return intent.getByteArrayExtra(EXTRA_DATA);
    }

    public static int getID(Intent intent)
    {
        return intent.getIntExtra(EXTRA_ID, ZentriOSBLEManager.ID_INVALID);
    }

    public static Command getCommand(Intent intent)
    {
        return (Command)intent.getSerializableExtra(EXTRA_COMMAND);
    }

    public static int getResponseCode(Intent intent)
    {
        return intent.getIntExtra(EXTRA_RESPONSE_CODE, -1);
    }

    public static ErrorCode getErrorCode(Intent intent)
    {
        return (ErrorCode)intent.getSerializableExtra(EXTRA_ERROR);
    }

    public static String getVersion(Intent intent)
    {
        return intent.getStringExtra(EXTRA_VERSION);
    }

    public static String getDeviceName(Intent intent)
    {
        return intent.getStringExtra(EXTRA_NAME);
    }

    public static int getIntData(Intent intent)
    {
        return intent.getIntExtra(EXTRA_DATA, -1);
    }

    public static byte getByteData(Intent intent)
    {
        return intent.getByteExtra(EXTRA_DATA, (byte) 0);
    }
}