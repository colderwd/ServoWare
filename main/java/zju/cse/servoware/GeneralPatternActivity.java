package zju.cse.servoware;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import de.tavendo.autobahn.WebSocket;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketConnectionHandler;
import de.tavendo.autobahn.WebSocketException;

public class GeneralPatternActivity extends Activity implements Constant{

    private TextView mTv_showData;
    private byte cmdEnd[]={(byte)0x02,(byte)0x55,(byte)0x08,(byte)0x00,(byte)0x01,(byte)0x00,(byte)0x80,(byte)0x08,(byte)0x10,(byte)0x00,(byte)0x61,(byte)0xEF},
            readSysInfo[]={(byte)0x02,(byte)0x55,(byte)0x08,(byte)0x00,(byte)0x0B,(byte)0x00,(byte)0x43,(byte)0x00,(byte)0x60,(byte)0x00,(byte)0xDC,(byte)0x23},
            readRam2[]={(byte)0x02,(byte)0x55,(byte)0x08,(byte)0x00,(byte)0x01,(byte)0x00,(byte)0x80,(byte)0x08,(byte)0x10,(byte)0x00,(byte)0x61,(byte)0xEF},
            readCfg0[]={(byte)0x02,(byte)0x55,(byte)0x08,(byte)0x00,(byte)0x0F,(byte)0x00,(byte)0x00,(byte)0x40,(byte)0x00,(byte)0x00,(byte)0xAC,(byte)0x26},
            readCfg2[]={(byte)0x02,(byte)0x55,(byte)0x08,(byte)0x00,(byte)0x0F,(byte)0x00,(byte)0x20,(byte)0x40,(byte)0x00,(byte)0x00,(byte)0x12,(byte)0x42},
            readCfg4[]={(byte)0x02,(byte)0x55,(byte)0x08,(byte)0x00,(byte)0x0F,(byte)0x00,(byte)0x40,(byte)0x40,(byte)0x00,(byte)0x00,(byte)0xCA,(byte)0x27},
            readCfg6[]={(byte)0x02,(byte)0x55,(byte)0x08,(byte)0x00,(byte)0x0F,(byte)0x00,(byte)0x60,(byte)0x18,(byte)0x00,(byte)0x00,(byte)0x40,(byte)0x30},
            readRam3[]={(byte)0x02,(byte)0x55,(byte)0x08,(byte)0x00,(byte)0x0A,(byte)0x00,(byte)0x80,(byte)0x0A,(byte)0x70,(byte)0x00,(byte)0xF6,(byte)0xA6};
    private BluetoothCommunicateService mBluetoothCommunicateService = null;
    private static final String PREFS_NAME = "AutobahnAndroidEcho";
    static EditText mHostname;
    static EditText mPort;
    static TextView mStatusline;
    static Button mStart;

    private SharedPreferences mSettings;
    private int dataAddr,dataLen, recvdLen;
    private String recvdData="",dataSendWeb="";
    private boolean flag3;
    private void loadPrefs() {

        mHostname.setText(mSettings.getString("hostname", "222.205.33.27"));
        mPort.setText(mSettings.getString("port", "8080"));
    }

    private void savePrefs() {

        SharedPreferences.Editor editor = mSettings.edit();
        editor.putString("hostname", mHostname.getText().toString());
        editor.putString("port", mPort.getText().toString());
        editor.commit();
    }

    private void setButtonConnect() {
        mHostname.setEnabled(true);
        mPort.setEnabled(true);
        mStart.setText("Connect");
        mStart.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                start();
            }
        });
    }

    private void setButtonDisconnect() {
        mHostname.setEnabled(false);
        mPort.setEnabled(false);
        mStart.setText("Disconnect");
        mStart.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                mConnection.disconnect();
            }
        });
    }

    private final WebSocket mConnection = new WebSocketConnection();

    private void start() {

        final String wsuri = "ws://" + mHostname.getText() + ":" + mPort.getText();

        mStatusline.setText("Status: Connecting to " + wsuri + " ..");

        setButtonDisconnect();

        try {
            mConnection.connect(wsuri, new WebSocketConnectionHandler() {
                @Override
                public void onOpen() {
                    mStatusline.setText("Status: Connected to " + wsuri);
                    savePrefs();
                }

                @Override
                public void onTextMessage(String payload) {
                    Toast.makeText(getApplicationContext(), "Got cmd"+payload, Toast.LENGTH_SHORT).show();
                    recvdLen = 0;
                    recvdData = "";  dataSendWeb = "";
                    if(payload.startsWith("readData")){
                        String[] cmdFrame = payload.split(",");
                        dataAddr = Integer.parseInt(cmdFrame[1], 16);
                        dataLen = Integer.parseInt(cmdFrame[2],16);
                        if(dataLen == 100){
                            dataLen = 0x60;
                        }
                        mBluetoothCommunicateService.write(Util.rdDatCmmandLine(dataAddr, dataLen));
                    }else if(payload.equals("readTrace")){
                        //必须先停止电机并清除故障状态（OnRstFaultRecord() ）
                        mBluetoothCommunicateService.write(Util.specialFuncCommandLine(CMDCODE_SysCommand, (byte) 14));
                        Toast.makeText(getApplicationContext(), "响应曲线数据读取中...", Toast.LENGTH_LONG).show();
                        try {
                            Thread.sleep(50);	//等待电机停止
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        dataAddr = 0; dataLen = 100;
                        mBluetoothCommunicateService.write(Util.rdDatCmmandLine(dataAddr, dataLen));
                    }else if(payload.equals("readCfgData")){
                        dataLen = 0x40;
                        mBluetoothCommunicateService.write(readCfg0);
                    }else if(payload.startsWith("readRam")){
                        if(payload.length()==13){
                            dataLen = 0x70;
                            flag3 = true;
                            mBluetoothCommunicateService.write(readRam3);
                        }else if(payload.length()==12){
                            flag3 = false;
                        }

                    }
                    mTv_showData.setText("");

                }

                @Override
                public void onClose(int code, String reason) {
                    Log.e("onClose","Connection lost.");
                    mStatusline.setText("Status: Ready.");
                    setButtonConnect();
                }
            });
        } catch (WebSocketException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("ReadRawData", "+ ON CREATE +");
        setContentView(R.layout.activity_general_pattern);
        mTv_showData = (TextView)findViewById(R.id.tv_data);
        mTv_showData.setMovementMethod(ScrollingMovementMethod.getInstance());
        mHostname = (EditText) findViewById(R.id.hostname);
        mPort = (EditText) findViewById(R.id.port);
        mStatusline = (TextView) findViewById(R.id.statusline);
        mStart = (Button) findViewById(R.id.start);

        mSettings = getSharedPreferences(PREFS_NAME, 0);
        loadPrefs();

        setButtonConnect();

        mBluetoothCommunicateService = new BluetoothCommunicateService(mHandler);

    }




    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("ReadRawData", "+ ON DESTROY +");

        // Stop the Bluetooth chat services
        if (mBluetoothCommunicateService != null) {
            mBluetoothCommunicateService.write(cmdEnd);
            mBluetoothCommunicateService.stop();
        }

        if (mConnection.isConnected()) {
            mConnection.disconnect();
        }
    }


    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer   new String(readBuf, 0, msg.arg1)
                    String readMessage = Util.byte2Str(readBuf, msg.arg1);
                    Log.i("READ", "message received : " + readMessage);
                    recvdData += readMessage;
                    recvdLen += msg.arg1;
                    Log.e("recvdLen", "" + recvdLen);
                    if(recvdLen == dataLen+12){
                        recvdLen = 0;
                        String[] frame = recvdData.split(", ");
                        if(CRC16.isCRCChecked(frame)){
                            Log.e("SHOW", "Checked" );
                            switch (Integer.parseInt(frame[4],16)){
                                case CMDCODE_READRAM:
                                    if(dataLen == 100){
                                        try {
                                            Thread.sleep(30);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        dataSendWeb+=recvdData.substring(40,recvdData.length()-8);
                                        recvdData = "";
                                        if((dataAddr+=50)<1024){
                                            mBluetoothCommunicateService.write(Util.rdDatCmmandLine(dataAddr,100));
                                        }else {
                                            mConnection.sendTextMessage(dataSendWeb);
                                            mTv_showData.setText(dataSendWeb);
                                        }
                                    }else {
                                        mConnection.sendTextMessage(recvdData);
                                        mTv_showData.setText(recvdData);
                                    }
                                    break;
                                case CMDCODE_READCFG:
                                    switch (Integer.parseInt(frame[6],16)){
                                        case 0:
                                            dataSendWeb+=recvdData.substring(40,recvdData.length()-8);
                                            recvdData = "";
                                            mBluetoothCommunicateService.write(readCfg2); //发送下一条指令
                                            break;
                                        case 0x20:
                                            dataSendWeb+=recvdData.substring(40,recvdData.length()-8);
                                            recvdData = "";
                                            mBluetoothCommunicateService.write(readCfg4); //发送下一条指令
                                            break;
                                        case 0x40:
                                            dataSendWeb+=recvdData.substring(40,recvdData.length()-8);
                                            recvdData = "";
                                            dataLen = 0x18;
                                            mBluetoothCommunicateService.write(readCfg6); //发送下一条指令
                                            break;
                                        case 0x60:
                                            dataSendWeb+=recvdData.substring(40,recvdData.length()-8);
                                            recvdData = "";
                                            mConnection.sendTextMessage(dataSendWeb);
                                            mTv_showData.setText(dataSendWeb);
                                            break;
                                    }
                                    break;
                                case CMDCODE_TestData:
                                    mConnection.sendTextMessage(recvdData.substring(40,recvdData.length()-8));
                                    mTv_showData.setText(recvdData);
                                    if(flag3){
                                        try {
                                            Thread.sleep(1000);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        recvdLen = 0;
                                        recvdData = "";
                                        mBluetoothCommunicateService.write(readRam3);
                                    }
                                    break;
                            }

                        }else {
                            Log.e("SHOW", "Check failed");
                            //mBluetoothCommunicateService.write(Util.rdDatCmmandLine(dataAddr, dataLen));
                            //break;
                        }
                    }
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };


}
