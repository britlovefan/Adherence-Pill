package com.adherence.adherence;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.adherence.adherence.Connection.DBHelper;
import com.zentri.zentri_ble.BLECallbacks.ReceiveMode;
import com.zentri.zentri_ble_command.Command;
import com.zentri.zentri_ble_command.ErrorCode;
import com.zentri.zentri_ble_command.GPIODirection;
import com.zentri.zentri_ble_command.GPIOFunction;
import com.zentri.zentri_ble_command.Result;
import com.zentri.zentri_ble_command.ZentriOSBLEManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class MainActivity2 extends Activity  {
    private static final long SCAN_PERIOD = 5000;
    private static final long CONNECT_TIMEOUT_MS = 20000;
    private static final String TAG = "SmartCap";
    private final int BLE_ENABLE_REQ_CODE = 1;

    private final boolean NO_TX_NOTIFY_DISABLE = false;

    private ProgressDialog mConnectProgressDialog;
    private DeviceList mDeviceList;
    private Button mScanButton;
    private Button mFinishButton;

    private Handler mHandler;
    private Runnable mStopScanTask;
    private Runnable mConnectTimeoutTask;

    private ZentriOSBLEManager mZentriOSBLEManager;
    private boolean mConnecting = false;
    private boolean mConnected = false;
    private boolean mErrorDialogShowing = false;

    private String mCurrentDeviceName;

    private ServiceConnection mConnection;
    private ZentriOSBLEService mService;
    private boolean mBound = false;

    private boolean bound = false;

    private LocalBroadcastManager mLocalBroadcastManager;
    private BroadcastReceiver mBroadcastReceiver;
    private IntentFilter mReceiverIntentFilter;

    // Connected stuff
    private ToggleButton mModeButton;
    private int mCurrentMode;
    private Button mSendTextButton;
    private EditText mTextToSendBox;
    private TextView mReceivedDataTextBox;
    private ScrollView mScrollView;
    private Button mClearTextButton;
    private ToggleButton mToggleIm;
    private Button mShowIm;
    private ImageView imView;

    private ProgressDialog mDisconnectDialog;
    private boolean mDisconnecting = false;
    private Runnable mDisconnectTimeoutTask;

    private boolean x = false;

    private boolean mRecording = false;
    private String mFileNameLog;
    private byte[] imBytesSplit;
    private byte[] imBytes;
    private int count_bytes;
    private int len_image;
    private int val;
    private boolean header_done = false;

    private String temp = "";
    private Boolean gi = false;

    Calendar timeNow;

    int RSSI;
    ArrayList<String> values = new ArrayList<String>();
    ArrayAdapter<String> newadapter;

    DBHelper mydb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        initScanButton();
        initDeviceList();
        initBroadcastManager();
        initServiceConnection();
        initBroadcastReceiver();
        initReceiverIntentFilter();
        //Try to initiate the alarm and th

        startService(new Intent(this, ZentriOSBLEService.class));
        mHandler = new Handler();
        mStopScanTask = new Runnable() {
            @Override
            public void run() {
                stopScan();
            }
        };
        mConnectTimeoutTask = new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showErrorDialog(R.string.error, R.string.con_timeout_message);
                        dismissConnectDialog();
                        if (mZentriOSBLEManager != null && mZentriOSBLEManager.isConnected()) {
                            disconnect(NO_TX_NOTIFY_DISABLE);
                        }
                    }
                });
            }
        };

        mModeButton = (ToggleButton) findViewById(R.id.toggle_str_comm);
        mModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mModeButton.isChecked()) {
                    mCurrentMode = ZentriOSBLEManager.MODE_STREAM;
                } else {
                    mCurrentMode = ZentriOSBLEManager.MODE_COMMAND_REMOTE;
                }
                x = mZentriOSBLEManager.setMode(mCurrentMode);
                Log.d(TAG, "Mode set to: " + mCurrentMode);
                Log.d(TAG, "Truconnect Manager returned: " + x);
            }
        });
        mTextToSendBox = (EditText) findViewById(R.id.editText);
        mSendTextButton = (Button) findViewById(R.id.button_send);
        mSendTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String data = mTextToSendBox.getText().toString();
                String dataToSend = "0";
                if (mCurrentMode == ZentriOSBLEManager.MODE_STREAM && data != null && !data.isEmpty()) {
                    if (data.equals("R")) { // READ RTC
                        dataToSend = "*R#";
                        mZentriOSBLEManager.writeData(dataToSend);
                        Log.d(TAG, "Sent: " + dataToSend);
                    } else if (data.equals("T")) { // SET RTC
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
                        mZentriOSBLEManager.writeData(dataToSend);
                        Log.d(TAG, "Sent: " + dataToSend);

                    } else if (data.equals("C")) { // TAKE IMAGE
                        dataToSend = "*C#";
                        mZentriOSBLEManager.writeData(dataToSend);
                        Log.d(TAG, "Sent: " + dataToSend);
                    } else if (data.startsWith("*")) { // Own command
                        dataToSend = data;
                        mZentriOSBLEManager.writeData(dataToSend);
                        Log.d(TAG, "Sent: " + dataToSend);
                    } else {
                        dataToSend = data;
                        mZentriOSBLEManager.writeData(dataToSend);
                        Log.d(TAG, "Sent: " + dataToSend);
                    }
                }

                if (mCurrentMode == ZentriOSBLEManager.MODE_COMMAND_REMOTE) {
                    if (data.isEmpty()) {
                        //mZentriOSBLEManager.GPIOGetUsage();
                    } else if (Integer.parseInt(data) == 1) {
                        mZentriOSBLEManager.GPIOFunctionSet(10, GPIOFunction.CONN_GPIO);
                        mZentriOSBLEManager.GPIOFunctionSet(11, GPIOFunction.STDIO);
                        mZentriOSBLEManager.GPIODirectionSet(11, GPIODirection.HIGH_IMPEDANCE);
                    } else if (Integer.parseInt(data) == 0) {
                        mZentriOSBLEManager.save();
                        mZentriOSBLEManager.reboot();
                    }
                }
                mTextToSendBox.setText("");//clear input after send
            }
        });
        mClearTextButton = (Button) findViewById(R.id.clear_button);
        mToggleIm = (ToggleButton) findViewById(R.id.toggle_im);
        mToggleIm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mToggleIm.isChecked()) {
                    // TODO start logging
                    doStartRecording();
                    startRecording();
                    count_bytes = 0;
                    header_done = false;
                } else {
                    // TODO stop logging
                    doStopRecording();
                    stopRecording();
                }
                Log.d(TAG, "Image recording: " + mRecording);
            }
        });
        // to display data
        mReceivedDataTextBox = (TextView) findViewById(R.id.receivedDataBox);

        mScrollView = (ScrollView) findViewById(R.id.scroll_view);
        GUISetCommandMode();//set up gui for command mode initially

        mDisconnectTimeoutTask = new Runnable() {
            @Override
            public void run() {
                dismissProgressDialog();
                showErrorDialog(R.string.error, R.string.discon_timeout_message);
            }
        };
    //add the valid device of the user to store to database.
        ListView currentView = (ListView) findViewById(R.id.currentList);
        ListView scanView = (ListView) findViewById(R.id.scanList);
        initialiseListviewListener(scanView);
        newadapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, values);
        currentView.setAdapter(newadapter);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_about:
                openAboutDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mDeviceList.clear();
        mConnected = false;
        mConnecting = false;
        Intent intent = new Intent(this, ZentriOSBLEService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, mReceiverIntentFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        mHandler.removeCallbacks(mStopScanTask);
        cancelConnectTimeout();
        dismissConnectDialog();
        if (bound) {
            bound = false;
        }
        if (mBound) {
            mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
            unbindService(mConnection);
            mBound = false;
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, ZentriOSBLEService.class));
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BLE_ENABLE_REQ_CODE) {
            mService.initTruconnectManager();//try again
            if (mZentriOSBLEManager.isInitialised()) {
                startScan();
            } else {
                showUnrecoverableErrorDialog(R.string.init_fail_title, R.string.init_fail_msg);
            }
        }
    }

    private void initScanButton() {
        mScanButton = (Button) findViewById(R.id.button_scan);
        mScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDeviceList.clear();
                startScan();
            }
        });
        mFinishButton = (Button) findViewById(R.id.finishAdd);
        mFinishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDeviceList.clear();
                //pass the arraylist of device name to the alarm manager and then pass to the BLEService
                scheduleAlarm(values);
                //try to jump to another activity
                /*
                Intent triggerNext = new Intent(MainActivity2.this,NextActivity.class);
                startActivity(triggerNext);*/
            }
        });
    }

    private void initDeviceList() {
        ListView deviceListView = (ListView) findViewById(R.id.scanList);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.listitem, R.id.textView);
        initialiseListviewListener(deviceListView);
        mDeviceList = new DeviceList(adapter, deviceListView);
    }

    private void initServiceConnection() {
        mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                ZentriOSBLEService.LocalBinder binder = (ZentriOSBLEService.LocalBinder) service;
                mService = binder.getService();
                mBound = true;
                mZentriOSBLEManager = mService.getManager();
                if (!mZentriOSBLEManager.isInitialised()) {
                    startBLEEnableIntent();
                } else {
                    startScan();
                }
            }
            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                mBound = false;
            }
        };
    }
    //receive message from ZentriOSBLEService and perform actions
    private void initBroadcastReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Get extra data included in the Intent
                String action = intent.getAction();
                Log.d(TAG, "Received intent " + intent);
                switch (action) {
                    case ZentriOSBLEService.ACTION_SCAN_RESULT:
                        addDeviceToList(ZentriOSBLEService.getData(intent));
                        break;

                    case ZentriOSBLEService.ACTION_CONNECTED:
                        String deviceName = ZentriOSBLEService.getDeviceName(intent);
                        int services = ZentriOSBLEService.getIntData(intent);
                        cancelConnectTimeout();
                        dismissConnectDialog();
                        //RSSI = intent.getShortExtra(mBroadcastReceiver.EXTRA_RSSI,Short.MIN_VALUE);
                        //if no truconnect services
                        if (services == ZentriOSBLEService.SERVICES_NONE ||
                                services == ZentriOSBLEService.SERVICES_OTA_ONLY) {
                            showErrorDialog(R.string.error, R.string.error_service_disc);
                            disconnect(NO_TX_NOTIFY_DISABLE);
                        } else if (!mConnected) {
                            mConnected = true;
                            showToast("Connected to " + deviceName, Toast.LENGTH_SHORT);
                            Log.d(TAG, "Connected to " + deviceName);
                            startDeviceInfoActivity();
                        }
                        break;

                    case ZentriOSBLEService.ACTION_DISCONNECTED:
                        setDisconnectedState();
                        dismissConnectDialog();
                        cancelConnectTimeout();
                        break;

                    case ZentriOSBLEService.ACTION_COMMAND_SENT:
                        String command = ZentriOSBLEService.getCommand(intent).toString();
                        Log.d(TAG, "Command " + command + " sent");
                        break;


                    case ZentriOSBLEService.ACTION_COMMAND_RESULT:
                        handleCommandResponse(intent);
                        break;

                    case ZentriOSBLEService.ACTION_MODE_WRITE:
                        int mode = ZentriOSBLEService.getMode(intent);
                        if (mode == ZentriOSBLEManager.MODE_STREAM) {
                            //disable buttons while in stream mode (must be in rem command to work)
                            GUISetStreamMode();
                        } else {
                            GUISetCommandMode();
                        }
                        break;

                    case ZentriOSBLEService.ACTION_STRING_DATA_READ:
                        //if (mCurrentMode == ZentriOSBLEManager.MODE_STREAM)
                        String text = ZentriOSBLEService.getData(intent);
                        updateReceivedTextBox(text);
                        //what does it means?
                        if (text.equals("I")) {
                            mZentriOSBLEManager.setReceiveMode(ReceiveMode.BINARY);
                            count_bytes = 0;
                            header_done = false;
                            break;
                        }
                        if (text.equals("N")) {
                            break;
                        }
                        Log.d(TAG, "text = : " + text);
                          /*
                            if (gi == true) {
                                String dataToSend = "*ai#";
                                mZentriOSBLEManager.writeData(dataToSend);
                                gi = false;
                            } else {
                                String dataToSend = "*gi#";
                                mZentriOSBLEManager.writeData(dataToSend);
                                gi = true;
                            }*/

                            Log.d(TAG, "Bytes: " + count_bytes);
                        break;

                        //}

                    case ZentriOSBLEService.ACTION_BINARY_DATA_READ:
                        byte[] block = ZentriOSBLEService.getBinaryData(intent);
                        if (mRecording) {
                            writeLog(block.toString());
                        }
                        if (!header_done) {
                            if (block.length == 1 && count_bytes == 0) {
                                len_image = block[0];
                                count_bytes++;
                            } else if (block.length == 1 && count_bytes == 1) {
                                len_image += block[0] * 256;
                                count_bytes--;
                                header_done = true;
                                imBytes = new byte[len_image];
                            } else if (block.length > 1) {
                                len_image = (block[0] & 0x00FF) | ((block[1] << 8) & 0xFF00);
                                header_done = true;
                                imBytes = new byte[len_image];

                                if (block.length > 2) {
                                    System.arraycopy(block, 2, imBytes, count_bytes, block.length - 2);
                                    count_bytes += block.length - 2;
                                }
                            }
                        } else {
                            if (count_bytes + block.length > len_image) {
                                System.arraycopy(block, 0, imBytes, count_bytes, len_image - count_bytes);
                                count_bytes += (len_image - count_bytes);
                            } else {
                                System.arraycopy(block, 0, imBytes, count_bytes, block.length);
                                count_bytes += block.length;
                            }
                        }
                        //if (count_bytes < len_image) mZentriOSBLEManager.writeData("0");
                        //mZentriOSBLEManager.writeData("0");

                        if (count_bytes > 2 && imBytes[count_bytes - 2] == -1 && imBytes[count_bytes - 1] == -39) {
                            //if (count_bytes>=(len_image) && header_done) {
                            saveImage(imBytes);
                            mToggleIm.setChecked(false);
                            doStopRecording();
                            stopRecording();
                            clearReceivedTextBox();
                            Log.d(TAG, "SDFADSFASDFASFASFASFASFA   photo show");
                            imView = (ImageView) findViewById(R.id.imageView);
                            Bitmap bmp = BitmapFactory.decodeByteArray(imBytes, 0, len_image);
                            imView.setImageBitmap(bmp);
                            mZentriOSBLEManager.setReceiveMode(ReceiveMode.STRING);
                        }
                        Log.d(TAG, "Bytes: " + count_bytes + "Val: " + block[0]);
                        gi  = true;
                        break;

                    case ZentriOSBLEService.ACTION_ERROR:
                        ErrorCode errorCode = ZentriOSBLEService.getErrorCode(intent);
                        Log.v("ERRORCODE",errorCode+"");
                        //handle errors
                        switch (errorCode) {
                            case CONNECT_FAILED:
                                setDisconnectedState();
                                dismissConnectDialog();
                                cancelConnectTimeout();
                                showErrorDialog(R.string.error, R.string.con_err_message);
                                break;

                            case DEVICE_ERROR:
                                cancelConnectTimeout();
                                dismissConnectDialog();
                                if (mZentriOSBLEManager != null && mZentriOSBLEManager.isConnected()) {
                                    disconnect(NO_TX_NOTIFY_DISABLE);
                                }
                                break;

                            case SET_NOTIFY_FAILED:
                                cancelConnectTimeout();
                                dismissConnectDialog();
                                disconnect(NO_TX_NOTIFY_DISABLE);
                                showErrorDialog(R.string.error, R.string.error_configuration);
                                break;

                            case SERVICE_DISCOVERY_ERROR:
                                //need to disconnect
                                cancelConnectTimeout();
                                dismissConnectDialog();
                                disconnect(NO_TX_NOTIFY_DISABLE);
                                showErrorDialog(R.string.error, R.string.error_service_disc);
                                break;

                            case DISCONNECT_FAILED:
                                if (!isFinishing()) {
                                    showUnrecoverableErrorDialog(R.string.error, R.string.discon_err_message);
                                }
                                break;
                        }
                        break;
                }
            }
        };
    }

   //schedule the alarm which runs every hour and trigger the connectService.class which deals with connection
    public void scheduleAlarm(ArrayList<String> device) {
        Intent intent = new Intent(getApplicationContext(), myAlarmReceiver.class);
        intent.putStringArrayListExtra("DEVICE_NAME", device);
        final PendingIntent pIntent = PendingIntent.getBroadcast(this, myAlarmReceiver.REQUEST_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        long firstMillis = System.currentTimeMillis(); // alarm is set right away
        AlarmManager alarm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, firstMillis,
                AlarmManager.INTERVAL_HOUR, pIntent);
    }

    public void initBroadcastManager() {
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
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

    private void startBLEEnableIntent() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, BLE_ENABLE_REQ_CODE);
    }

    private void initialiseListviewListener(ListView listView) {
        final DBHelper mydb = new DBHelper(this);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                mCurrentDeviceName = mDeviceList.get(position);
                if(!values.contains(mCurrentDeviceName)) {
                    values.add(mCurrentDeviceName);
                    //add to current database
                    mydb.addDevice("christina", mCurrentDeviceName);
                }
                newadapter.notifyDataSetChanged();
            }
        });
    }
    private void startScan() {
        if (mZentriOSBLEManager != null) {
            Toast.makeText(getApplicationContext(), "Manager is not null", Toast.LENGTH_LONG).show();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mZentriOSBLEManager.startScan();
                }
            });
            disableScanButton();
            mHandler.postDelayed(mStopScanTask, SCAN_PERIOD);
        } else {
            Toast.makeText(getApplicationContext(), "Manager is null", Toast.LENGTH_LONG).show();
        }
    }

    private void stopScan() {
        if (mZentriOSBLEManager != null && mZentriOSBLEManager.stopScan()) {
            enableScanButton();
        }
    }

    private void startDeviceInfoActivity() {
        //GUISetCommandMode();
        mDeviceList.clear();
        /*
        mZentriOSBLEManager.setMode(ZentriOSBLEManager.MODE_COMMAND_REMOTE);
        mZentriOSBLEManager.setSystemCommandMode(CommandMode.MACHINE);
        mZentriOSBLEManager.getVersion();
        mCurrentMode = ZentriOSBLEManager.MODE_STREAM;
        x = mZentriOSBLEManager.setMode(mCurrentMode);*/
        Toast.makeText(getApplicationContext(), "here!", Toast.LENGTH_LONG).show();
    }
    //下面的代码基本上是处理错误信息之类的可以以后再看
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void enableScanButton() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mScanButton.setEnabled(true);
            }
        });
    }

    private void disableScanButton() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mScanButton.setEnabled(false);
            }
        });
    }

    private void showToast(final String msg, final int duration) {
        if (!isFinishing()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), msg, duration).show();
                }
            });
        }
    }

    private void showErrorDialog(final int titleID, final int msgID) {
        if (!mErrorDialogShowing && !isFinishing()) {
            mErrorDialogShowing = true;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity2.this);
                    builder.setTitle(titleID)
                            .setMessage(msgID)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    mErrorDialogShowing = false;
                                }
                            })
                            .create()
                            .show();
                }
            });
        }
    }

    private void showUnrecoverableErrorDialog(final int titleID, final int msgID) {
        if (!isFinishing()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity2.this);

                    builder.setTitle(titleID)
                            .setMessage(msgID)
                            .setPositiveButton(R.string.exit, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    finish();
                                }
                            })
                            .create()
                            .show();
                }
            });
        }
    }

    private void dismissConnectDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mConnectProgressDialog != null) {
                    mConnectProgressDialog.dismiss();
                    mConnectProgressDialog = null;
                }
            }
        });
    }
    //Only adds to the list if not already in it
    private void addDeviceToList(final String name) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDeviceList.add(name);
            }
        });
    }
    private void showConnectingDialog(final Context context) {
        if (!isFinishing()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final ProgressDialog dialog = new ProgressDialog(MainActivity2.this);
                    String title = getString(R.string.progress_title);
                    String msg = getString(R.string.progress_message);
                    dialog.setIndeterminate(true);//Dont know how long connection could take.....
                    dialog.setCancelable(true);

                    mConnectProgressDialog = dialog.show(context, title, msg);
                    mConnectProgressDialog.setCancelable(true);
                    mConnectProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialogInterface) {
                            dialogInterface.dismiss();
                        }
                    });
                }
            });
        }
    }

    private void setDisconnectedState() {
        mConnected = false;
        mConnecting = false;
    }

    private void cancelConnectTimeout() {
        mHandler.removeCallbacks(mConnectTimeoutTask);
    }

    private void disconnect(boolean disableTxNotify) {
        setDisconnectedState();
        mZentriOSBLEManager.disconnect(disableTxNotify);
    }

    private void openAboutDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Dialogs.makeAboutDialog(MainActivity2.this).show();
            }
        });
    }

    private void dismissProgressDialog() {
        if (mDisconnectDialog != null) {
            mDisconnectDialog.dismiss();
        }
    }
    private void handleCommandResponse(Intent intent) {
        Command command = ZentriOSBLEService.getCommand(intent);
        int ID = ZentriOSBLEService.getID(intent);
        int code = ZentriOSBLEService.getResponseCode(intent);
        String result = ZentriOSBLEService.getData(intent);
        String message = "";
        Log.d(TAG, "Command " + command + " result");
        if (code == Result.SUCCESS) {
            switch (command) {
                case ADC:
                    /*if (ID == mCurrentADCCommandID)
                    {
                        message = String.format("ADC: %s", result);
                        mADCTextView.setText(message);
                    }
                    else
                    {
                        Log.d(TAG, "Invalid ADC command ID!");
                    }*/
                    break;

                case GPIO_GET:
                    /*if (ID == mCurrentGPIOCommandID)
                    {
                        message = String.format("GPIO: %s", result);
                        mGPIOTextView.setText(message);
                    }
                    else
                    {
                        Log.d(TAG, "Invalid GPIO command ID!");
                    }*/
                    break;

                case GPIO_SET:
                    break;
            }
        } else {
            message = String.format("ERROR %d - %s", code, result);
            showToast(message, Toast.LENGTH_SHORT);
        }
    }


    //set up gui elements for command mode operation
    private void GUISetCommandMode() {
        //mSendTextButton.setEnabled(false);
        //mTextToSendBox.setVisibility(View.INVISIBLE);
    }

    //set up gui elements for command mode operation
    private void GUISetStreamMode() {
        mSendTextButton.setEnabled(true);
        mTextToSendBox.setVisibility(View.VISIBLE);
    }

    private void updateReceivedTextBox(String newData) {
        mReceivedDataTextBox.append(newData);
    }

    private void clearReceivedTextBox() {
        mReceivedDataTextBox.setText("");
    }

    private void doStartRecording() {
        File sdCard = Environment.getExternalStorageDirectory();

        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String currentDateTimeString = format.format(new Date());
        String fileName = sdCard.getAbsolutePath() + "/ams001_" + currentDateTimeString + ".log";

        this.setFileNameLog(fileName);
        this.startRecording();

        showToast("Logging Started", Toast.LENGTH_SHORT);
    }

    private void doStopRecording() {
        this.stopRecording();
//        showToast("Logging Stopped", Toast.LENGTH_SHORT);
    }

    public void setFileNameLog(String fileNameLog) {
        mFileNameLog = fileNameLog;
    }

    public void startRecording() {
        mRecording = true;
    }

    public void stopRecording() {
        mRecording = false;
    }

    private boolean writeLog(String buffer) {
        String state = Environment.getExternalStorageState();
        File logFile = new File(mFileNameLog);

        if (Environment.MEDIA_MOUNTED.equals(state)) {

            try {
                FileOutputStream f = new FileOutputStream(logFile, true);

                PrintWriter pw = new PrintWriter(f);
                pw.print(buffer);
                pw.flush();
                pw.close();

                f.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            this.stopRecording();
            return false;
        } else {
            this.stopRecording();
            return false;
        }

        return true;
    }

    private void saveImage(byte[] data) {
        File sdCard = Environment.getExternalStorageDirectory();
        String fileName = sdCard.getAbsolutePath() + "/ams001_image.jpg";
        File testimage = new File(fileName);

        if (testimage.exists()) {
            testimage.delete();
        }

        try {
            FileOutputStream fos = new FileOutputStream(testimage.getPath());
            fos.write(data);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
