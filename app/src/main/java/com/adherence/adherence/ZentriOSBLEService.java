package com.adherence.adherence;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.adherence.adherence.Connection.DBHelper;
import com.parse.ParseObject;
import com.zentri.zentri_ble_command.BLECallbacks;
import com.zentri.zentri_ble_command.Command;
import com.zentri.zentri_ble_command.CommandMode;
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
    private String response;
    private IntentFilter mReceiverIntentFilter;
    private BroadcastReceiver mBroadcastReceiver;
    private boolean newData;
    private final boolean disableTxNotify = false;
    private boolean mConnected = false;
    private boolean receivedStringResponse = false;
    private int mCurrentMode;
    private String mCurrentDeviceName;
    private boolean gi;

    //Create its own receiver but How do I interact with it in the loop
    private void initBroadCastReceiver(){
        mBroadcastReceiver = new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch (action) {
                    case ZentriOSBLEService.ACTION_STRING_DATA_READ:
                        String text = ZentriOSBLEService.getData(intent);
                        Log.v("String data",text+"");
                        receivedStringResponse = true;
                        if (text.equals("N")) {
                            Log.v("ZentriReceiver","No New Data!");
                            newData = false;
                            break;
                        }
                        //
                        if(text.equals("Y")){
                            newData = true;
                        }
                        if(text.endsWith("Units")){

                        }
                        if(text.endsWith("mAh")){

                        }
                        if(text.endsWith("V")){

                        }
                        if (text.contains(":")) {
                            ParseObject testObject = new ParseObject("TestXZ");
                            testObject.put("TIME", text);
                            //testObject.put("NAME", mCurrentDeviceName);
                            testObject.saveEventually();
                        }
                        /*
                        if (text.equals("I")) {
                            mZentriOSBLEManager.setReceiveMode(com.zentri.zentri_ble.BLECallbacks.ReceiveMode.BINARY);
                            break;
                        }*/
                        break;
                    case ZentriOSBLEService.ACTION_CONNECTED:
                        String deviceName = ZentriOSBLEService.getDeviceName(intent);
                        int services = ZentriOSBLEService.getIntData(intent);
                        if (services == ZentriOSBLEService.SERVICES_NONE ||
                                services == ZentriOSBLEService.SERVICES_OTA_ONLY) {
                            Log.d("ZentriOSBLEService","Failed to discover TruConnect services");
                            //showErrorDialog(R.string.error, R.string.error_service_disc);
                            mZentriOSBLEManager.disconnect(disableTxNotify);
                        } else if (!mConnected) {
                            mConnected = true;
                            Log.d(TAG, "Connected to " + deviceName);
                            mZentriOSBLEManager.setSystemCommandMode(CommandMode.MACHINE);
                            mZentriOSBLEManager.setMode(ZentriOSBLEManager.MODE_COMMAND_REMOTE);
                            mZentriOSBLEManager.setSystemCommandMode(CommandMode.MACHINE);
                            mZentriOSBLEManager.getVersion();
                        }
                        break;
                }
            }
        };
    }
    public class LocalBinder extends Binder
    {
        ZentriOSBLEService getService()
        {
            // Return this instance of LocalService so clients can call public methods
            return ZentriOSBLEService.this;
        }
    }

    /*The system invokes this method to perform one-time setup procedures when the service is initially created
     before it calls either onStartCommand() or onBind()). If the service is already running, this method is not called.
     */
    @Override
    public void onCreate()
    {
        // The service is being created
        Log.d(TAG, "Creating service");
        // Set up the broadcast receiver
        mZentriOSBLEManager = new ZentriOSBLEManager();
        mBroadcastManager = LocalBroadcastManager.getInstance(this);
        initReceiverIntentFilter();
        initBroadCastReceiver();
        mBroadcastManager.registerReceiver(mBroadcastReceiver, mReceiverIntentFilter);

        initCallbacks();
        initTruconnectManager();
    }
    public void initReceiverIntentFilter() {
        mReceiverIntentFilter = new IntentFilter();
        mReceiverIntentFilter.addAction(ZentriOSBLEService.ACTION_SCAN_RESULT);
        mReceiverIntentFilter.addAction(ZentriOSBLEService.ACTION_CONNECTED);
        mReceiverIntentFilter.addAction(ZentriOSBLEService.ACTION_DISCONNECTED);
        mReceiverIntentFilter.addAction(ZentriOSBLEService.ACTION_ERROR);
        mReceiverIntentFilter.addAction(ZentriOSBLEService.ACTION_COMMAND_RESULT);
        mReceiverIntentFilter.addAction(ZentriOSBLEService.ACTION_STRING_DATA_READ);
        mReceiverIntentFilter.addAction(ZentriOSBLEService.ACTION_BINARY_DATA_READ);
        mReceiverIntentFilter.addAction(ZentriOSBLEService.ACTION_MODE_WRITE);
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
                        //check if connected
                        while (!mConnected) {
                            Thread.sleep(50);
                        }
                        setTime();

                        //change to stream mode before sending the "*gn#"
                        mCurrentMode = ZentriOSBLEManager.MODE_STREAM;
                        mZentriOSBLEManager.setMode(mCurrentMode);
                        mZentriOSBLEManager.setReceiveMode(com.zentri.zentri_ble.BLECallbacks.ReceiveMode.STRING);

                        mZentriOSBLEManager.writeData("*gn#");
                        //wait until there are string responds


                        if(newData==false){
                           Log.v(TAG,"No New Data");
                        }
                        while(newData) {
                                Log.v(TAG, "New Data Here");
                                if (gi == true) {
                                    String dataToSend = "*ai#";
                                    mZentriOSBLEManager.writeData(dataToSend);
                                    gi = false;
                                }
                                if (gi = false) {
                                    String dataToSend = "*gi#";
                                    mZentriOSBLEManager.writeData(dataToSend);
                                }
                                mZentriOSBLEManager.writeData("*gn#");
                                Thread.sleep(300);
                        }
                        //disconnect the device and move to the next one
                        mZentriOSBLEManager.disconnect(true);
                    } catch (Exception e) {

                    }

                }
            };
            t.start();
        }
    }
    //send and set the current time to the bottle every time we connect to the bottle
    public void setTime(){
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("ssmmHHddmmyyyy");
        String strDate = sdf.format(c.getTime());
        String timeCommand = "*st" + strDate + "#";
        mZentriOSBLEManager.writeData(timeCommand);
        Log.v("Set Time correctly!",strDate);
    }
    private void startDeviceInfoActivity() {
        /*
        mZentriOSBLEManager.setMode(ZentriOSBLEManager.MODE_COMMAND_REMOTE);
        mZentriOSBLEManager.setSystemCommandMode(CommandMode.MACHINE);*/
        setTime();
    }
    /* if you choose to implement the onStartCommand() callback method, then you must explicitly stop the service, because the service
     is now considered to be started. In this case, the service runs
     until the service stops itself with stopSelf() or another component calls stopService(), regardless
     of whether it is bound to any clients.
      */
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
        /* must return an integer. The integer is a value that describes how the system
         should continue the service in the event that the system kills it.
         */
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

        mBroadcastManager.unregisterReceiver(mBroadcastReceiver);

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