package zju.cse.servoware;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Random;

import de.tavendo.autobahn.WebSocket;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketConnectionHandler;
import de.tavendo.autobahn.WebSocketException;

public class GeneralPatternActivity extends Activity implements Constant{

    private TextView mTv_showData;
    private byte
            //readSysInfo[]={(byte)0x02,(byte)0x55,(byte)0x08,(byte)0x00,(byte)0x0B,(byte)0x00,(byte)0x43,(byte)0x00,(byte)0x60,(byte)0x00,(byte)0xDC,(byte)0x23},
            readRam[]={(byte)0x02,(byte)0x55,(byte)0x08,(byte)0x00,(byte)0x01,(byte)0x00,(byte)0x80,(byte)0x08,(byte)0x10,(byte)0x00,(byte)0x61,(byte)0xEF},
            readCfg0[]={(byte)0x02,(byte)0x55,(byte)0x08,(byte)0x00,(byte)0x0F,(byte)0x00,(byte)0x00,(byte)0x40,(byte)0x00,(byte)0x00,(byte)0xAC,(byte)0x26},
            readCfg2[]={(byte)0x02,(byte)0x55,(byte)0x08,(byte)0x00,(byte)0x0F,(byte)0x00,(byte)0x20,(byte)0x40,(byte)0x00,(byte)0x00,(byte)0x12,(byte)0x42},
            readCfg4[]={(byte)0x02,(byte)0x55,(byte)0x08,(byte)0x00,(byte)0x0F,(byte)0x00,(byte)0x40,(byte)0x40,(byte)0x00,(byte)0x00,(byte)0xCA,(byte)0x27},
            readCfg6[]={(byte)0x02,(byte)0x55,(byte)0x08,(byte)0x00,(byte)0x0F,(byte)0x00,(byte)0x60,(byte)0x18,(byte)0x00,(byte)0x00,(byte)0x40,(byte)0x30},
            readRam3[]={(byte)0x02,(byte)0x55,(byte)0x08,(byte)0x00,(byte)0x0A,(byte)0x00,(byte)0x80,(byte)0x0A,(byte)0x70,(byte)0x00,(byte)0xF6,(byte)0xA6};
    private BluetoothCommunicateService mBluetoothCommunicateService = null;
    private static final String PREFS_NAME = "AutobahnAndroidEcho";
    static TextView mStatusline;
    static Button mStart;
    private String url = "http://servoware.applinzi.com/transfer.php";
    private String response;
    private SharedPreferences mSettings;
    private int dataAddr,dataLen, recvdLen;
    private String recvdData="",dataSendWeb="",self="",to="";
    private boolean flag3 = false;


    private void setButtonConnect() {
        mStart.setText("Connect");
        mStart.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                start();
            }
        });
    }

    private void setButtonDisconnect() {
        mStart.setText("Disconnect");
        mStart.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                mConnection.disconnect();
            }
        });
    }

    private final WebSocket mConnection = new WebSocketConnection();

    private void start() {

        //final String wsuri = response;

        mStatusline.setText("Status: Connecting to Webservoware ..");

        setButtonDisconnect();

        try {
            mConnection.connect(response, new WebSocketConnectionHandler() {
                @Override
                public void onOpen() {
                    mStatusline.setText("Status: Connected to Webservoware");
                    //savePrefs(); ?channelurl可保存？判断失效？?
                    mTv_showData.setText("远程模式已打开,正在等待受理...");
                    //getHttpResponse(url, "POST", "action=androidConnect&self="+self, 5000, 5000);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            getHttpResponse(url, "POST", "action=androidConnect&self="+self, 5000, 5000);
                        }
                    }).start();
                }

                @Override
                public void onTextMessage(String payload) {
                     recvdLen = 0;
                     recvdData = "";  dataSendWeb = "";
                     try {
                         if(payload.startsWith("connect")){
                             Toast.makeText(getApplicationContext(), "连接已建立", Toast.LENGTH_SHORT).show();
                             String[] fromTo = payload.split(",");
                             if(fromTo.length==2){
                                 to = fromTo[1];
                                 mTv_showData.setText("已受理，等待接受远程指令中...");
                                 recvdLen = 0;  recvdData="";
                                 dataLen = 0x10; //下一条指令的读字节数
                                 mBluetoothCommunicateService.write(readRam); //发送下一条指令
                             }else{
                                 Toast.makeText(getApplicationContext(), "建立连接出错！",
                                         Toast.LENGTH_SHORT).show();
                                 mTv_showData.setText("建立连接出错！请重新连接");
                             }
                         }else{
                             if(!to.equals("")){
                                 if (payload.equals("startReadRam")) {  //--------------------readRam-------------------------------
                                     Toast.makeText(getApplicationContext(), "Got cmd readRam", Toast.LENGTH_SHORT).show();
                                     dataLen = 0x70;
                                     flag3 = true;
                                     mBluetoothCommunicateService.write(readRam3);
                                 }else{
                                     if(flag3){
                                         flag3 = false;
                                     }else{
                                         if (payload.startsWith("readRawData")) {   //--------------------readData-------------------------------
                                             Toast.makeText(getApplicationContext(), "Got cmd readRaw", Toast.LENGTH_SHORT).show();
                                             String[] cmdFrame = payload.split(",");
                                             dataAddr = Integer.parseInt(cmdFrame[1], 16);
                                             dataLen = Integer.parseInt(cmdFrame[2], 16);
                                             if (dataLen == 100) {
                                                 dataLen = 0x60;
                                             }
                                             mBluetoothCommunicateService.write(Util.rdDatCmmandLine(dataAddr, dataLen));
                                         } else if (payload.equals("readTrace")) {   //--------------------readTrace-------------------------------
                                             //必须先停止电机并清除故障状态（OnRstFaultRecord() ）
                                             mBluetoothCommunicateService.write(Util.specialFuncCommandLine(CMDCODE_SysCommand, (byte) 14));
                                             Toast.makeText(getApplicationContext(), "响应曲线数据读取中...", Toast.LENGTH_LONG).show();
                                             try {
                                                 Thread.sleep(50);    //等待电机停止
                                             } catch (InterruptedException e) {
                                                 e.printStackTrace();
                                             }
                                             dataAddr = 0;
                                             dataLen = 100;
                                             mBluetoothCommunicateService.write(Util.rdDatCmmandLine(dataAddr, dataLen));
                                         } else if (payload.equals("readCfgData")) {  //--------------------readCfgData-------------------------------
                                             Toast.makeText(getApplicationContext(), "Got cmd readCfg", Toast.LENGTH_SHORT).show();
                                             dataLen = 0x40;
                                             mBluetoothCommunicateService.write(readCfg0);
                                         } else if (payload.startsWith("DownloadCfg")) {  //--------------------DownloadCfg-------------------------------
                                             Toast.makeText(getApplicationContext(), "Got cmd DownloadCfg", Toast.LENGTH_SHORT).show();
                                             String[] cfgs = payload.split(",");
                                             if (cfgs.length == 105) {
                                                 byte[] cfg = new byte[208];
                                                 for (int i = 1; i < 105; i++) {
                                                     int cfgData = Integer.parseInt(cfgs[i]);
                                                     cfg[2 * i - 2] = (byte) (cfgData & 0xff);
                                                     cfg[2 * i - 1] = (byte) ((cfgData & 0xff00) >> 8);
                                                 }
                                                 mBluetoothCommunicateService.write(Util.wrCfgCommandLine(Arrays.copyOfRange(cfg, 0, 64), 0));
                                                 try {
                                                     Thread.sleep(100);
                                                 } catch (InterruptedException e) {
                                                     e.printStackTrace();
                                                 }
                                                 mBluetoothCommunicateService.write(Util.wrCfgCommandLine(Arrays.copyOfRange(cfg, 64, 128), 0x20));
                                                 try {
                                                     Thread.sleep(100);
                                                 } catch (InterruptedException e) {
                                                     e.printStackTrace();
                                                 }
                                                 mBluetoothCommunicateService.write(Util.wrCfgCommandLine(Arrays.copyOfRange(cfg, 128, 192), 0x40));
                                                 try {
                                                     Thread.sleep(100);
                                                 } catch (InterruptedException e) {
                                                     e.printStackTrace();
                                                 }
                                                 mBluetoothCommunicateService.write(Util.wrCfgCommandLine(Arrays.copyOfRange(cfg, 192, 208), 0x60));
                                                 try {
                                                     Thread.sleep(100);
                                                 } catch (InterruptedException e) {
                                                     e.printStackTrace();
                                                 }
                                                 Toast.makeText(getApplicationContext(), "组态下载完毕！",
                                                         Toast.LENGTH_SHORT).show();
                                                 dataLen = 0x40;
                                                 mBluetoothCommunicateService.write(readCfg0);
                                             }
                                         }else if (payload.equals("cfgInit")) {  //--------------------initCfg-------------------------------
                                             Toast.makeText(getApplicationContext(), "Got cmd initCfg", Toast.LENGTH_SHORT).show();
                                             mBluetoothCommunicateService.write(Util.specialFuncCommandLine(CMDCODE_SysCommand, (byte) 15));
                                             try {
                                                 Thread.sleep(1000);
                                             } catch (InterruptedException e) {
                                                 e.printStackTrace();
                                             }
                                             Toast.makeText(getApplicationContext(), "组态初始成功！",
                                                     Toast.LENGTH_SHORT).show();
                                             dataLen = 0x40;
                                             mBluetoothCommunicateService.write(readCfg0);
                                         }
                                     }
                                 }
                             }
                         }
                     }catch (Exception e){
                         Toast.makeText(getApplicationContext(), "parse error", Toast.LENGTH_SHORT).show();
                     }
                    mTv_showData.setText("");

                }

                @Override
                public void onClose(int code, String reason) {
                    Log.e("onClose", "Connection lost.");
                    mStatusline.setText("Status: Ready.");
                    mTv_showData.setText("本地连接已断开");
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            getHttpResponse(url, "POST", "action=androidDisconnect&self="+self+"&to="+to, 5000, 5000);
                        }
                    }).start();

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
        //StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        //StrictMode.setThreadPolicy(policy);
        Random r = new Random();
        for(int i=0; i<11; i++){
            self += r.nextInt(10);
        }
        TextView deviceId = (TextView)findViewById(R.id.textView2);
        deviceId.setText("设备"+self.substring(0,5));
        new Thread(new Runnable() {
            @Override
            public void run() {
                String content = "action=androidChannel&self="+self;
                response  = getHttpResponse(url, "POST", content, 2000, 2000);
                Message msg = new Message();
                msg.what = MESSAGE_WS;
                msg.obj = response;
                mHandler.sendMessage(msg);
            }
        }).start();
    }




    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("ReadRawData", "+ ON DESTROY +");

        // Stop the Bluetooth chat services
        if (mBluetoothCommunicateService != null) {
            mBluetoothCommunicateService.write(readRam);
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
                        String[] frame = recvdData.split(",");
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
                                        dataSendWeb=recvdData.substring(30,recvdData.length()-6);
                                        new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                getHttpResponse(url, "POST", "action=a2w&data=TRA"+dataSendWeb+"&to="+to, 2000, 2000);
                                            }
                                        }).start();
                                        recvdData = "";
                                        if((dataAddr+=50)<1024){
                                            mBluetoothCommunicateService.write(Util.rdDatCmmandLine(dataAddr,100));
                                        }else {
                                            mTv_showData.setText("动态曲线数据读取并发送完毕！");
                                        }
                                    }else if(dataLen == 0x10){
                                        dataSendWeb = frame[14];
                                        new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                getHttpResponse(url, "POST", "action=androidConnected&data=CIP"+dataSendWeb+"&to="+to, 2000, 2000);
                                            }
                                        }).start();

                                        recvdData ="";
                                        mTv_showData.setText("设备信息发送完毕！");
                                    }
                                    break;
                                case CMDCODE_READCFG:
                                    switch (Integer.parseInt(frame[6],16)){
                                        case 0:
                                            dataSendWeb+=recvdData.substring(30,recvdData.length()-6);
                                            recvdData = "";
                                            mBluetoothCommunicateService.write(readCfg2); //发送下一条指令
                                            break;
                                        case 0x20:
                                            dataSendWeb+=recvdData.substring(30,recvdData.length()-6);
                                            recvdData = "";
                                            mBluetoothCommunicateService.write(readCfg4); //发送下一条指令
                                            break;
                                        case 0x40:
                                            dataSendWeb+=recvdData.substring(30,recvdData.length()-6);
                                            recvdData = "";
                                            dataLen = 0x18;
                                            mBluetoothCommunicateService.write(readCfg6); //发送下一条指令
                                            break;
                                        case 0x60:
                                            dataSendWeb+=recvdData.substring(30,recvdData.length()-6);
                                            recvdData = "";
                                            //mConnection.sendTextMessage(dataSendWeb);

                                            new Thread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    getHttpResponse(url, "POST", "action=a2w&data=CFG"+dataSendWeb+"&to="+to, 2000, 2000);
                                                }
                                            }).start();
                                            mTv_showData.setText("系统配置参数读取并发送完毕！");
                                            break;
                                    }
                                    break;
                                case CMDCODE_TestData:
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            getHttpResponse(url, "POST", "action=a2w&data=RAM"+recvdData.substring(30, recvdData.length()-6)+"&to="+to, 2000, 2000);
                                        }
                                    }).start();

                                    mTv_showData.setText("系统特性读取和发送中...");
                                    if(flag3){
                                        try {
                                            Thread.sleep(500);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        recvdLen = 0;
                                        recvdData = "";
                                        mBluetoothCommunicateService.write(readRam3);
                                    }else {
                                        mTv_showData.setText("系统特性读取已停止.");
                                    }
                                    break;
                            }

                        }else {
                            Log.e("SHOW", "Check failed");
                            if(flag3){
                                recvdLen = 0;
                                recvdData = "";
                                mBluetoothCommunicateService.write(readRam3);
                            }
                        }
                    }
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_WS:
                    if(response!=null && response.startsWith("ws://channel.sinaapp.com/com/")){
                        mTv_showData = (TextView)findViewById(R.id.tv_data);
                        mTv_showData.setText("点击按钮打开远程模式");
                        mStatusline = (TextView) findViewById(R.id.statusline);
                        mStart = (Button) findViewById(R.id.start);

                        mSettings = getSharedPreferences(PREFS_NAME, 0);

                        setButtonConnect();

                        mBluetoothCommunicateService = new BluetoothCommunicateService(mHandler);
                    }else{
                        Toast.makeText(getApplicationContext(), "网络异常！\n" +
                                "请检查网络后重新打开本页面", Toast.LENGTH_SHORT).show();
                        mTv_showData = (TextView)findViewById(R.id.tv_data);
                        mTv_showData.setText("网络异常！\n请检查网络后重新打开本页面");
                    }
                    break;
            }
        }
    };

    private String getHttpResponse(String urlStr, String method, String content,
                                   int connectTimeout, int readTimeout) {
        HttpURLConnection conn = null;
        InputStream in = null;
        String res = "";
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(connectTimeout);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");
            conn.setReadTimeout(readTimeout);
            conn.connect();
            if ("POST".equals(method)) {
                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes(content);
                dos.flush();
                dos.close();
            }

            in = conn.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line = "";
            while (null != (line = br.readLine())) {
                res += line;
            }

            Log.v("post back", "res: " + res);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            res = null;
        } catch (IOException e) {
            e.printStackTrace();
            res = null;
        } finally {
            if (null != conn)
                conn.disconnect();
            if (null != in)
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

        return res;
    }
}
