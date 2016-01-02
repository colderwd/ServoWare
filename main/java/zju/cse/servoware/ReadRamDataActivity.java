package zju.cse.servoware;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

public class ReadRamDataActivity extends Activity implements Constant{

    private TextView mTv_wdt,mTv_rpm, mTv_checkFail;
    private byte send[]={(byte)0x02,(byte)0x55,(byte)0x08,(byte)0x00,(byte)0x0A,(byte)0x00,(byte)0x80,(byte)0x0A,(byte)0x70,(byte)0x00,(byte)0xF6,(byte)0xA6},
                cmdEnd[]={(byte)0x02,(byte)0x55,(byte)0x08,(byte)0x00,(byte)0x01,(byte)0x00,(byte)0x80,(byte)0x08,(byte)0x10,(byte)0x00,(byte)0x61,(byte)0xEF},
                addrHigh = 0,addrLow;
    private int mReadLen ,mRecvdCount;
    private StringBuffer mRecvdData = new StringBuffer();
    private String mReceived[], mDataRecv[];
    private BluetoothCommunicateService mBluetoothCommunicateService = null;
    private boolean timerFlag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("ReadRawData", "+ ON CREATE +");
        setContentView(R.layout.activity_read_ram_data);
        initViews();

        mReadLen = 0x70;
        mBluetoothCommunicateService = new BluetoothCommunicateService(mHandler);
        timerFlag = false;
    }

    private void initViews(){
        mTv_wdt = (TextView)findViewById(R.id.tv_wdt);
        mTv_rpm  = (TextView)findViewById(R.id.tv_rpm);
        mTv_checkFail = (TextView)findViewById(R.id.tv_checkFail);
        mTv_checkFail.setFocusable(true);
        mTv_checkFail.setFocusableInTouchMode(true);
        mTv_checkFail.requestFocus();
        mTv_checkFail.requestFocusFromTouch();

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("ReadRawData", "+ ON DESTROY +");
        // Stop the Bluetooth chat services
        if (mBluetoothCommunicateService != null){
            mBluetoothCommunicateService.write(cmdEnd);
            mBluetoothCommunicateService.stop();

        }

    }

    public void onClick_readData(View view){
        if (BluetoothConnectService.getState() != STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
        }
        else {
            timerFlag = !timerFlag;
            if(timerFlag){
                sendCmd();
                ((Button)view).setText("停止");
            }else {
                ((Button)view).setText("通讯");
            }
        }
    }

    private void sendCmd(){
        //清空原数据
        mRecvdCount = 0;
        mRecvdData.setLength(0);
        //读取命令参数
        if(addrHigh != 0){
            send[6] = addrLow;
            send[7] = addrHigh;
        }
        short checkNum = CRC16.CRC_16(send,0, send.length-2);
        send[send.length-1] = (byte) (checkNum >> 8);
        send[send.length-2] = (byte) (checkNum & 0xff);
        mBluetoothCommunicateService.write(send);
    }

    private void showData(){
        ((EditText)findViewById(R.id.et_01)).setText(String.format("0x %04x", Integer.parseInt(mDataRecv[1] + mDataRecv[0], 16)));
        ((EditText)findViewById(R.id.et_02)).setText(String.format("%.2f", ((float)Integer.parseInt(mDataRecv[3] + mDataRecv[2], 16)/(0x1000))*100));
        ((EditText)findViewById(R.id.et_03)).setText(String.format("%.2f", (float)Integer.parseInt(mDataRecv[5] + mDataRecv[4], 16)));
        ((EditText)findViewById(R.id.et_04)).setText(String.format("%04d", Integer.parseInt(mDataRecv[7] + mDataRecv[6], 16)));
        ((EditText)findViewById(R.id.et_05)).setText(String.format("0x %04x", Integer.parseInt(mDataRecv[9] + mDataRecv[8], 16)));
        int IoSignal = Integer.parseInt(mDataRecv[11] + mDataRecv[10], 16);
        ((EditText)findViewById(R.id.et_06)).setText(String.format("0x %04x", IoSignal));
        int HostFlag = Integer.parseInt(mDataRecv[13] + mDataRecv[12], 16);
        ((EditText)findViewById(R.id.et_07)).setText(String.format("0x %04x", HostFlag));//HostFlag
        int HostFlag2 = Integer.parseInt(mDataRecv[15] + mDataRecv[14], 16);
        ((EditText)findViewById(R.id.et_08)).setText(String.format("0x %04x", HostFlag2));//HostFlag2
        ((EditText)findViewById(R.id.et_09)).setText(String.format("%01d", Integer.valueOf(mDataRecv[17] + mDataRecv[16], 16).shortValue()));
        int MotorSpeed = Integer.valueOf(mDataRecv[19] + mDataRecv[18], 16).shortValue();
        ((EditText)findViewById(R.id.et_010)).setText(String.format("%01d", MotorSpeed));//Motorspeed
        int sewVel = Integer.valueOf(mDataRecv[21] + mDataRecv[20], 16).shortValue();
        ((EditText)findViewById(R.id.et_011)).setText(String.format("%01d", sewVel));
        ((EditText)findViewById(R.id.et_012)).setText(String.format("%01d", Integer.parseInt(mDataRecv[23] + mDataRecv[22], 16)));
        ((EditText)findViewById(R.id.et_013)).setText(String.format("%01d", Integer.parseInt(mDataRecv[25] + mDataRecv[24], 16)));
        ((EditText)findViewById(R.id.et_014)).setText(String.format("%01d", Integer.parseInt(mDataRecv[27] + mDataRecv[26], 16)));
        ((EditText)findViewById(R.id.et_015)).setText(String.format("0x %04x", Integer.parseInt(mDataRecv[29] + mDataRecv[28], 16)));
        ((EditText)findViewById(R.id.et_016)).setText(String.format("%01d", Integer.parseInt(mDataRecv[31] + mDataRecv[30], 16)));
        ((EditText)findViewById(R.id.et_017)).setText(String.format("%.2f", ((float)Integer.parseInt(mDataRecv[33] + mDataRecv[32], 16)/0x3f5)*100));
        ((EditText)findViewById(R.id.et_018)).setText(String.format("%01d", Integer.parseInt(mDataRecv[35] + mDataRecv[34], 16)));
        ((EditText)findViewById(R.id.et_019)).setText(String.format("%01d", Integer.parseInt(mDataRecv[37] + mDataRecv[36], 16)));
        ((EditText)findViewById(R.id.et_020)).setText(String.format("%01d", Integer.parseInt(mDataRecv[39] + mDataRecv[38], 16)));
        float SV_PV_Rate = (float)Integer.parseInt(mDataRecv[39] + mDataRecv[38], 16);
        int data21 = Integer.parseInt(mDataRecv[41] + mDataRecv[40], 16);
        if(data21!=0)
            SV_PV_Rate =SV_PV_Rate/((float)data21);
        ((EditText)findViewById(R.id.et_021)).setText(String.format("%01d",data21 ));
        int Rotation_Dir = Integer.parseInt(mDataRecv[43] + mDataRecv[42], 16);
        ((EditText)findViewById(R.id.et_022)).setText(String.format("0x %04x", Rotation_Dir));
        ((EditText)findViewById(R.id.et_023)).setText(String.format("0x %04x", Integer.parseInt(mDataRecv[45] + mDataRecv[44], 16)));
        ((EditText)findViewById(R.id.et_024)).setText(String.format("%.2f", ((float)Integer.parseInt(mDataRecv[47] + mDataRecv[46], 16)/100)*100));
        ((EditText)findViewById(R.id.et_025)).setText(String.format("%01d", Integer.parseInt(mDataRecv[49] + mDataRecv[48], 16)));
        ((EditText)findViewById(R.id.et_026)).setText(String.format("%05d", Integer.parseInt(mDataRecv[51] + mDataRecv[50], 16)));
        ((EditText)findViewById(R.id.et_027)).setText(String.format("%01d", Integer.parseInt(mDataRecv[53] + mDataRecv[52], 16)));
        int CurrentLimitCount = Integer.parseInt(mDataRecv[55] + mDataRecv[54], 16);
        ((EditText)findViewById(R.id.et_028)).setText(String.format("%01d", CurrentLimitCount));
        ((EditText)findViewById(R.id.et_029)).setText(String.format("%01d", Integer.parseInt(mDataRecv[57] + mDataRecv[56], 16)));
        ((EditText)findViewById(R.id.et_030)).setText(String.format("%01d", Integer.parseInt(mDataRecv[59] + mDataRecv[58], 16)));
        ((EditText)findViewById(R.id.et_031)).setText(String.format("%01d", Integer.parseInt(mDataRecv[61] + mDataRecv[60], 16)));
        ((EditText)findViewById(R.id.et_032)).setText(String.format("%01d", Integer.parseInt(mDataRecv[63] + mDataRecv[62], 16)));
        ((EditText)findViewById(R.id.et_033)).setText(String.format("%01d", Integer.parseInt(mDataRecv[65] + mDataRecv[64], 16)));
        ((EditText)findViewById(R.id.et_034)).setText(String.format("%01d", Integer.parseInt(mDataRecv[67] + mDataRecv[66], 16)));
        int OutSignal = Integer.parseInt(mDataRecv[69] + mDataRecv[68], 16);
        ((EditText)findViewById(R.id.et_035)).setText(String.format("%01d", OutSignal));
        ((EditText)findViewById(R.id.et_036)).setText(String.format("%.0f", (float)Integer.parseInt(mDataRecv[71] + mDataRecv[70], 16)*100/0x3ff));
        ((EditText)findViewById(R.id.et_037)).setText(String.format("%01d", Integer.parseInt(mDataRecv[73] + mDataRecv[72], 16)));
        ((EditText)findViewById(R.id.et_038)).setText(String.format("%.0f", (float)Integer.parseInt(mDataRecv[75] + mDataRecv[74], 16)*100/4096));
        ((EditText)findViewById(R.id.et_039)).setText(String.format("%01d", Integer.parseInt(mDataRecv[77] + mDataRecv[76], 16)));
        ((EditText)findViewById(R.id.et_040)).setText(String.format("%01d", Integer.parseInt(mDataRecv[79] + mDataRecv[78], 16)));
        ((EditText)findViewById(R.id.et_041)).setText(String.format("%01d", Integer.parseInt(mDataRecv[81] + mDataRecv[80], 16)));
        int SewRotateS = Integer.parseInt(mDataRecv[83] + mDataRecv[82], 16);
        ((EditText)findViewById(R.id.et_042)).setText(String.format("%01d", SewRotateS));
        ((EditText)findViewById(R.id.et_043)).setText(String.format("%01d", Integer.parseInt(mDataRecv[85] + mDataRecv[84], 16)));
        int CheckStatus = Integer.parseInt(mDataRecv[87] + mDataRecv[86], 16);
        ((EditText)findViewById(R.id.et_044)).setText(String.format("0x %04x", CheckStatus));
        ((EditText)findViewById(R.id.et_045)).setText(String.format("%01d", Integer.parseInt(mDataRecv[89] + mDataRecv[88], 16)));
        ((EditText)findViewById(R.id.et_046)).setText(String.format("%01d", Integer.parseInt(mDataRecv[91] + mDataRecv[90], 16)));
        ((EditText)findViewById(R.id.et_047)).setText(String.format("%01d", Integer.parseInt(mDataRecv[93] + mDataRecv[92], 16)));
        ((EditText)findViewById(R.id.et_048)).setText(String.format("%.0f", (float)Integer.parseInt(mDataRecv[95] + mDataRecv[94], 16)/4096.0*100));
        ((EditText)findViewById(R.id.et_049)).setText(String.format("%.0f",  (float)SewRotateS/(360*4)));
        ((EditText)findViewById(R.id.et_050)).setText(String.format("%.0f", SV_PV_Rate));
        ((EditText)findViewById(R.id.et_051)).setText(String.format("%01d", Integer.parseInt(mDataRecv[97] + mDataRecv[96], 16)));
        int X5043WDTStatus = Integer.parseInt(mDataRecv[99] + mDataRecv[98], 16);		//x5043 WDT 状态
        if((X5043WDTStatus & 0x30) == 0x30){
            mTv_wdt.setText("WDT_OFF");
        }else{
            mTv_wdt.setText("WDT_ON");
        }

        StringBuffer FunSet = new StringBuffer();
        if(Rotation_Dir == 0x0001){
            FunSet.append("运行方向CCW,");
        }
        else FunSet.append("运行方向 CW,");
        int rData = Integer.parseInt(mDataRecv[101] + mDataRecv[100], 16);
        if((rData&0x01)!=0){
            FunSet.append("剪线 ON,");
        }
        else FunSet.append("剪线OFF,");
        if((rData&0x02)!=0){
            FunSet.append("中途停车抬压脚 ON,");
        }
        else FunSet.append("中途停车抬压脚OFF,");
        if((rData&0x04)!=0){
            FunSet.append("切线停车抬压脚 ON,");
        }else FunSet.append("切线停车抬压脚OFF,");
        if((rData&0x08)!=0){
            FunSet.append("针位 DN,");
        }
        else FunSet.append("针位 UP,");
        if((rData&0x10)!=0){
            FunSet.append("慢启动 ON");
        }
        else FunSet.append("慢启动OFF");
        ((TextView)findViewById(R.id.tv_funSet)).setText(FunSet);
        ((EditText) findViewById(R.id.et_052)).setText(String.format("%01d", Integer.parseInt(mDataRecv[103] + mDataRecv[102], 16)));
        mTv_rpm.setText(String.format("%01dRPM", Integer.parseInt(mDataRecv[105] + mDataRecv[104], 16)));
        ((EditText)findViewById(R.id.et_053)).setText(String.format("%01d", Integer.parseInt(mDataRecv[107] + mDataRecv[106], 16)));
        ((EditText)findViewById(R.id.et_054)).setText(String.format("%f", (float)(3*((float)Integer.parseInt(mDataRecv[111] + mDataRecv[110], 16)/1024.0)*10)));
        ((EditText)findViewById(R.id.et_055)).setText(String.format("%01d", (int)(MotorSpeed*((float)(360*4)/SewRotateS))));
        ((EditText)findViewById(R.id.et_056)).setText(String.format("%1d:%1d:%1d",IoSignal&0x01,(IoSignal&0x02)>>1,(IoSignal&0x04)>>2));
        ((EditText)findViewById(R.id.et_057)).setText(String.format("%1d:%1d",(IoSignal&0x0040)>>6,(IoSignal&0x0080)>>7));
        ((EditText)findViewById(R.id.et_059)).setText(String.format("%1d:%1d:%1d",(IoSignal&0x0800)>>11,(IoSignal&0x0400)>>10,(IoSignal&0x4000)>>14));
        ((EditText)findViewById(R.id.et_060)).setText(String.format("%1d",(IoSignal&0x0100)>>8));
        ((EditText)findViewById(R.id.et_061)).setText(String.format("%1d",(IoSignal&0x0200)>>9));
        ((EditText)findViewById(R.id.et_062)).setText(String.format("%1d:%1d",(IoSignal&0x10)>>4,(IoSignal&0x020)>>5));
        ((EditText)findViewById(R.id.et_063)).setText(String.format("%1d",(IoSignal&0x01000)>>12));
        ((EditText)findViewById(R.id.et_064)).setText(String.format("%1d",(IoSignal&0x02000)>>13));

        StringBuffer hostflag = new StringBuffer();
        if((HostFlag &0x0100)!=0)
        {							//m_para11:缝制速度
            if((HostFlag &0x1000) == 0&&(sewVel >3000)&&(CurrentLimitCount == 0)){
                hostflag.append("缝制进行中,电机工作电流不正常；  ");
            }else{
                hostflag.append("缝制进行中；");
            }
        }else{
            hostflag.append("缝制等待中；");
        }

        if((HostFlag2 &0x0200)!=0){
            hostflag.append("转动方向CW ;");
        }else {hostflag.append("转动方向CCW;");}

        if((HostFlag&(~0x0100))!=0){
            hostflag.append("系统故障: ");
        }else {hostflag.append("系统无故障;");}

        if((HostFlag &0x0001)!=0){
            hostflag.append("IO启动自检ERR,");
        }
        if((HostFlag &0x0002)!=0){
            hostflag.append("无上针位信号,");
        }
        if((HostFlag &0x0004)!=0){
            hostflag.append("系统断电,");
        }
        if((HostFlag &0x0008)!=0){
            hostflag.append("无下针位信号,");
        }
        if((HostFlag &0x0010)!=0){
            hostflag.append("IPM模块故障,");
        }
        if((HostFlag &0x0020)!=0){
            hostflag.append("直流母线电压过高报警,");
        }
        if((HostFlag &0x0040)!=0){
            hostflag.append("电磁铁过流保护,");
        }
        if((HostFlag &0x0080)!=0){
            hostflag.append("电机无法启动,");
        }
        if((HostFlag &0x0200)!=0){
            hostflag.append("直流母线电压过低,");
        }
        if((HostFlag &0x0400)!=0){
            hostflag.append("减速停车异常(超针数),");
        }
        if((HostFlag &0x0800)!=0){
            hostflag.append("速度越限报警,");
        }
        if((HostFlag &0x1000)!=0){
            hostflag.append("无同步器,");
        }
        if((HostFlag &0x2000)!=0){
            hostflag.append("E2内存读写异常,");
        }
        if((HostFlag &0x4000)!=0){
            hostflag.append("E2内存没有组态信息,");
        }
        if((HostFlag &0x8000)!=0){
            hostflag.append("编码器信号检测异常,");
        }
        if((HostFlag2 &0x0001)!=0){
            hostflag.append("皮带过松或转轮直径设置错误,");
        }
        if((HostFlag2 &0x0008)!=0){
            hostflag.append("参数设置错误,");		//密码设置错误，没有有效设置密码
        }
        if((HostFlag2 &0x0020)!=0){
            hostflag.append("写E2内存错误,");
        }
        if((HostFlag2 &0x0040)!=0){
            hostflag.append("自动测试时间到;");
        }
        if((HostFlag2 &0x0080)!=0){
            hostflag.append("手动出力调试");
        }
        if((HostFlag &0x0001)!=0){
            if((CheckStatus&0x0004)!=0){
                hostflag.append("断电信号;");
            }
            if((CheckStatus&0x0010)!=0){
                hostflag.append("过流信号信号错误;");
            }
            if((CheckStatus&0x0020)!=0){
                hostflag.append("母线电压信号高错误;");
            }
            if((CheckStatus&0x0200)!=0){
                hostflag.append("母线电压信号低错误;");
            }

            if((CheckStatus&0x1000)!=0){
                hostflag.append("安全开关信号错误;");
            }
            if((CheckStatus&0x2000)!=0){
                hostflag.append("脚踏PH2信号错误;");
            }
            if((CheckStatus&0x4000)!=0){
                hostflag.append("脚踏PH1信号错误;");
            }
            if((CheckStatus&0x8000)!=0){
                hostflag.append("脚踏行程信号过大错误;");
            }
        }
        ((TextView)findViewById(R.id.tv_hostFlag)).setText(hostflag);

        if((OutSignal &0x0001)!=0) ((EditText)findViewById(R.id.et_065)).setText("ON");
        if((OutSignal &0x0002)!=0) ((EditText)findViewById(R.id.et_066)).setText("ON");
        if((OutSignal &0x0004)!=0) ((EditText)findViewById(R.id.et_067)).setText("ON");
        if((OutSignal &0x0008)!=0) ((EditText)findViewById(R.id.et_068)).setText("ON");
        if((OutSignal &0x0010)!=0) ((EditText)findViewById(R.id.et_069)).setText("ON");
        if((OutSignal &0x0020)!=0) ((EditText)findViewById(R.id.et_070)).setText("ON");
        if((OutSignal &0x0040)!=0) ((EditText)findViewById(R.id.et_071)).setText("ON");
        if((OutSignal &0x0080)!=0) ((EditText)findViewById(R.id.et_072)).setText("ON");
        if((IoSignal&0x02000)!=0) ((EditText)findViewById(R.id.et_073)).setText("ON");	//PWM1-6输出使能，对应Con_EN引脚
        if((OutSignal &0x0200)!=0) ((EditText)findViewById(R.id.et_074)).setText("ON");
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
                    Log.i("READ", "Raw message received : " + readMessage);

                    mRecvdData.append(readMessage);
                    mRecvdCount+= msg.arg1;
                    Log.i("READ", "message received count: " + mRecvdCount + "/" + (mReadLen + 12));
                    if(mRecvdCount == (mReadLen+12)){
                        mReceived = mRecvdData.toString().split(", ");
                        if(mReceived.length == mRecvdCount && CRC16.isCRCChecked(mReceived)){

                            addrHigh = Integer.valueOf(mReceived[7],16).byteValue();
                            addrLow = Integer.valueOf(mReceived[6],16).byteValue();
                            if((Integer.valueOf(mReceived[22],16).byteValue()&0xef)!=0){
                                mTv_checkFail.setText("自检出错");
                            }else {
                                mTv_checkFail.setText("自检正常");
                            }
                            mDataRecv = Arrays.copyOfRange(mReceived, 10, mRecvdCount - 2);
                            Log.e("SHOW", "length: " + mDataRecv.length );
                            showData();
                            mRecvdData.setLength(0);
                            mRecvdCount=0;
                            if(timerFlag){
                                try {
                                    Thread.sleep(MainActivity.refreshPeriod);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                sendCmd();
                            }else {
                                Toast.makeText(getApplicationContext(), "已停止通讯", Toast.LENGTH_SHORT).show();
                            }
                        }else {
                            sendCmd();
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
