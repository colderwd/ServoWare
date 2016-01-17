package zju.cse.servoware;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends Activity implements Constant{


    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    public static int refreshPeriod =200;
    // Layout Views
    private TextView mTitle;
    private Spinner mSpinner_period;
    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothConnectService mConnectService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("Main", "+ ON CREATE +");
        // Set up the window layout
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.activity_main);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

        // Set up the custom title
        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle.setText(R.string.app_name);
        mTitle = (TextView) findViewById(R.id.title_right_text);
        mTitle.setFocusable(true);
        mTitle.setFocusableInTouchMode(true);
        mTitle.requestFocus();
        mTitle.requestFocusFromTouch();
        mSpinner_period = (Spinner) findViewById(R.id.spinner_refreshPeriod);
        ArrayAdapter<CharSequence> aaPeriod;
        aaPeriod = ArrayAdapter.createFromResource(this,
                R.array.period_list, android.R.layout.simple_spinner_item);
        aaPeriod.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner_period.setAdapter(aaPeriod);
        mSpinner_period.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int arg2, long arg3) {
                    refreshPeriod = Integer.valueOf(mSpinner_period.getSelectedItem().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {

            }

        });

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }else{
            Toast.makeText(getApplicationContext(), "请按菜单键选择设备连接！", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else {
            if (mConnectService == null)
                mConnectService = new BluetoothConnectService(mHandler);
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        Log.e("Main", "+ ON RESUME +");
        if (mConnectService != null)
            mConnectService.setState(BluetoothConnectService.mState);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("Main", "+ ON DESTROY +");
        // Stop the Bluetooth chat services
        if (BluetoothConnectService.mSocket != null && BluetoothConnectService.mSocket.isConnected()){
            try {
                BluetoothConnectService.mSocket.close();
                Log.e("Main", "+ ON DESTROY + close btSocket");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mConnectService != null) mConnectService.stop();
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothConnectService.STATE_CONNECTED:
                            mTitle.setText(R.string.title_connected_to);
                            mTitle.append(mConnectedDeviceName);
                            break;
                        case BluetoothConnectService.STATE_CONNECTING:
                            mTitle.setText(R.string.title_connecting);
                            break;
                        case BluetoothConnectService.STATE_NONE:
                            mTitle.setText(R.string.title_not_connected);
                            break;
                    }
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras()
                            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Get the BLuetoothDevice object
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    // Attempt to connect to the device
                    mConnectService.connect(device);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    mConnectService = new BluetoothConnectService(mHandler);
                } else {
                    // User did not enable Bluetooth or an error occured
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.scan:
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                return true;
        }
        return false;
    }


    public void onClick_ButtonReadRaw(View view){
        if (BluetoothConnectService.getState() != STATE_CONNECTED) {
            mConnectService.setState(STATE_NONE);
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
        }
        else {
            Intent intent = new Intent(this,ReadRawDataActivity.class);
            startActivity(intent);
        }
    }

    public void onClick_ButtonReadRam(View view){
        if (BluetoothConnectService.getState() != STATE_CONNECTED) {
            mConnectService.setState(STATE_NONE);
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
        }
        else {
            Intent intent = new Intent(this,ReadRamDataActivity.class);
            startActivity(intent);
        }
    }

    public void onClick_ButtonReadCfg(View view){
        if (BluetoothConnectService.getState() != STATE_CONNECTED) {
            mConnectService.setState(STATE_NONE);
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
        }
        else {
            Intent intent = new Intent(this,ReadCfgDataActivity.class);
            startActivity(intent);
        }
    }

    public void onClick_GeneralPattern(View view){
        if (BluetoothConnectService.getState() != STATE_CONNECTED) {
            mConnectService.setState(STATE_NONE);
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
        }
        else {
            Intent intent = new Intent(this,GeneralPatternActivity.class);
            startActivity(intent);
        }
    }
}
