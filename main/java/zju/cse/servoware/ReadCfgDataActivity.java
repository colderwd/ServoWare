package zju.cse.servoware;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

public class ReadCfgDataActivity extends Activity implements Constant{

    private TextView mTv_versionInfo;
    private Button mDownloadCfg,mInitCfg,mSaveToFile,mRestoreFromFile;
    private EditText mEt[] = new EditText[104];
    private byte readSysInfo[]={(byte)0x02,(byte)0x55,(byte)0x08,(byte)0x00,(byte)0x0B,(byte)0x00,(byte)0x43,(byte)0x00,(byte)0x60,(byte)0x00,(byte)0xDC,(byte)0x23},
            readRam[]={(byte)0x02,(byte)0x55,(byte)0x08,(byte)0x00,(byte)0x01,(byte)0x00,(byte)0x80,(byte)0x08,(byte)0x10,(byte)0x00,(byte)0x61,(byte)0xEF},
            readCfg0[]={(byte)0x02,(byte)0x55,(byte)0x08,(byte)0x00,(byte)0x0F,(byte)0x00,(byte)0x00,(byte)0x40,(byte)0x00,(byte)0x00,(byte)0xAC,(byte)0x26},
            readCfg2[]={(byte)0x02,(byte)0x55,(byte)0x08,(byte)0x00,(byte)0x0F,(byte)0x00,(byte)0x20,(byte)0x40,(byte)0x00,(byte)0x00,(byte)0x12,(byte)0x42},
            readCfg4[]={(byte)0x02,(byte)0x55,(byte)0x08,(byte)0x00,(byte)0x0F,(byte)0x00,(byte)0x40,(byte)0x40,(byte)0x00,(byte)0x00,(byte)0xCA,(byte)0x27},
            readCfg6[]={(byte)0x02,(byte)0x55,(byte)0x08,(byte)0x00,(byte)0x0F,(byte)0x00,(byte)0x60,(byte)0x18,(byte)0x00,(byte)0x00,(byte)0x40,(byte)0x30},
           mReceived[]= new byte[108], mRecvSysInfo[], mChipPARTID,
           mRecvCfg0[] = new byte[64],mRecvCfg2[] = new byte[64],mRecvCfg4[] = new byte[64],mRecvCfg6[] = new byte[24];
    private int  mDataLen, mRecvCount ,XE2type, failureSymbol;
    private StringBuffer VersionInfo = new StringBuffer();
    private BluetoothCommunicateService mBluetoothCommunicateService = null;
    private boolean etFlag;
    private SharedPreferences mPreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("ReadRawData", "+ ON CREATE +");
        setContentView(R.layout.activity_read_cfg_data);
        initViews();
        etFlag = false;
        mPreference = getSharedPreferences("cfgs", Context.MODE_PRIVATE);
        mBluetoothCommunicateService = new BluetoothCommunicateService(mHandler);
    }

    private void initViews(){
        mDownloadCfg = (Button)findViewById(R.id.button_downloadCfg);
        mDownloadCfg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int data;
                for(int i = 0;i<32;i++){  //cfg0----------------------------------------------------
                    if(( ((0x00ff&mRecvCfg0[2*i+1])<<8) + (0x00ff & mRecvCfg0[2*i]) ) != (data = Integer.parseInt(mEt[i].getText().toString())) ){
                        Log.e("data "+(1+i),data+"");
                        mRecvCfg0[2*i] = (byte)(data&0x00ff);
                        mRecvCfg0[2*i+1] = (byte)((data&0xff00)>>8);
                    }
                }
                mBluetoothCommunicateService.write(Util.wrCfgCommandLine(mRecvCfg0,0));
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                for(int i = 0;i<32;i++){  //cgf2----------------------------------------------------
                    if(( ((0x00ff&mRecvCfg2[2*i+1])<<8) + (0x00ff & mRecvCfg2[2*i]) ) != (data = Integer.parseInt(mEt[32+i].getText().toString())) ){
                        Log.e("data "+(33+i),data+"");
                        mRecvCfg2[2*i] = (byte)(data&0x00ff);
                        mRecvCfg2[2*i+1] = (byte)((data&0xff00)>>8);
                    }
                }
                mBluetoothCommunicateService.write(Util.wrCfgCommandLine(mRecvCfg2,0x20));
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                for(int i = 0;i<32;i++){  //cgf4----------------------------------------------------
                    if(( ((0x00ff&mRecvCfg4[2*i+1])<<8) + (0x00ff & mRecvCfg4[2*i]) ) != (data = Integer.parseInt(mEt[64+i].getText().toString())) ){
                        Log.e("data "+(65+i),data+"");
                        mRecvCfg4[2*i] = (byte)(data&0x00ff);
                        mRecvCfg4[2*i+1] = (byte)((data&0xff00)>>8);
                    }
                }
                mBluetoothCommunicateService.write(Util.wrCfgCommandLine(mRecvCfg4, 0x40));
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                for(int i = 0;i<8;i++){  //cfg6-----------------------------------------------------
                    if(( ((0x00ff&mRecvCfg6[2*i+1])<<8) + (0x00ff & mRecvCfg6[2*i]) ) != (data = Integer.parseInt(mEt[96+i].getText().toString())) ){
                        Log.e("data "+(97+i),data+"");
                        mRecvCfg6[2*i] = (byte)(data&0x00ff);
                        mRecvCfg6[2*i+1] = (byte)((data&0xff00)>>8);
                    }
                }
                mBluetoothCommunicateService.write(Util.wrCfgCommandLine(mRecvCfg6,0x60));
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Toast.makeText(getApplicationContext(), "组态下载完毕！",
                        Toast.LENGTH_SHORT).show();
                mDataLen = 0x60;
                failureSymbol = CMDCODE_READSYSINFO;
                mBluetoothCommunicateService.write(readSysInfo);
            }
        });

        mInitCfg = (Button)findViewById(R.id.button_initCfg);
        mInitCfg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBluetoothCommunicateService.write(Util.specialFuncCommandLine(CMDCODE_SysCommand, (byte) 15));
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Toast.makeText(getApplicationContext(), "组态初始成功！",
                        Toast.LENGTH_SHORT).show();
                mDataLen = 0x60;
                failureSymbol = CMDCODE_READSYSINFO;
                mBluetoothCommunicateService.write(readSysInfo);
            }
        });

        mSaveToFile = (Button)findViewById(R.id.button_saveTofile);
        mSaveToFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = mPreference.edit();
                editor.putString("cfg0", Util.byte2Str(mRecvCfg0, mRecvCfg0.length));
                editor.putString("cfg2", Util.byte2Str(mRecvCfg2, mRecvCfg2.length));
                editor.putString("cfg4", Util.byte2Str(mRecvCfg4, mRecvCfg4.length));
                editor.putString("cfg6", Util.byte2Str(mRecvCfg6, 16));
                editor.apply();
                Toast.makeText(getApplicationContext(), "配置参数已下载！",
                        Toast.LENGTH_SHORT).show();
            }
        });

        mRestoreFromFile = (Button)findViewById(R.id.button_dlSets);
        mRestoreFromFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String cfgofAll = mPreference.getString("cfg0","notInit"); //cfg0-------------------------------------------------
                if(!cfgofAll.equals("notInit")){
                    String[] cfgs = cfgofAll.split(",");
                    byte[] cfg = new byte[64];
                    for (int i = 0; i < 64; i++) {
                        cfg[i] = Integer.valueOf(cfgs[i],16).byteValue();
                    }
                    mBluetoothCommunicateService.write(Util.wrCfgCommandLine(cfg, 0));
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    cfgofAll = mPreference.getString("cfg2","notInit");//cfg2-------------------------------------------------
                    cfgs = cfgofAll.split(",");
                    for (int i = 0; i < 64; i++) {
                        cfg[i] = Integer.valueOf(cfgs[i],16).byteValue();
                    }
                    mBluetoothCommunicateService.write(Util.wrCfgCommandLine(cfg, 0x20));
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    cfgofAll = mPreference.getString("cfg4","notInit");//cfg4-------------------------------------------------
                    cfgs = cfgofAll.split(",");
                    for (int i = 0; i < 64; i++) {
                        cfg[i] = Integer.valueOf(cfgs[i],16).byteValue();
                    }
                    mBluetoothCommunicateService.write(Util.wrCfgCommandLine(cfg, 0x40));
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    cfgofAll = mPreference.getString("cfg6","notInit");//cfg6-------------------------------------------------
                    cfgs = cfgofAll.split(",");
                    for (int i = 0; i < 16; i++) {
                        cfg[i] = Integer.valueOf(cfgs[i],16).byteValue();
                    }
                    mBluetoothCommunicateService.write(Util.wrCfgCommandLine(Arrays.copyOfRange(cfg, 0, 16), 0x60));
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(getApplicationContext(), "组态下载完毕！",
                            Toast.LENGTH_SHORT).show();
                    mDataLen = 0x60;
                    failureSymbol = CMDCODE_READSYSINFO;
                    mBluetoothCommunicateService.write(readSysInfo);
                }else{
                    Toast.makeText(getApplicationContext(), "配置文件为空！",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        mTv_versionInfo = (TextView)findViewById(R.id.tv_versionInfo);
        mTv_versionInfo.setFocusable(true);
        mTv_versionInfo.setFocusableInTouchMode(true);
        mTv_versionInfo.requestFocus();
        mTv_versionInfo.requestFocusFromTouch();
    }


    private void showDataCfg0(byte[] cfg0){
        if(cfg0.length == 64){
            for(int i=0; i<32;i++){
                mEt[i].setText(String.format("%01d", ((0x00ff&cfg0[2*i+1])<<8) +(0x00ff &  cfg0[2*i] )));
            }
        }
    }

    private void showDataCfg2(byte[] cfg2){
        if(cfg2.length == 64){
            for(int i=0; i<32;i++){
                mEt[i+32].setText(String.format("%01d", ((0x00ff&cfg2[2*i+1])<<8) +(0x00ff &  cfg2[2*i] )));
            }
        }
    }

    private void showDataCfg4(byte[] cfg4){
        if(cfg4.length == 64){
            for(int i=0; i<32;i++){
                mEt[i+64].setText(String.format("%01d", ((0x00ff&cfg4[2*i+1])<<8) +(0x00ff &  cfg4[2*i] )));
            }
        }
    }

    private void showDataCfg6(byte[] cfg6){
        if(cfg6.length == 24){
            for(int i=0; i<8;i++){
                mEt[i+96].setText(String.format("%01d", ((0x00ff&cfg6[2*i+1])<<8) +(0x00ff &  cfg6[2*i] )));
            }
            StringBuffer serialNum = new StringBuffer(String.format("%04d", ((0x00ff&cfg6[19])<<8) +(0x00ff &  cfg6[18] )));
            serialNum.append("-").append(String.format("%04d", ((0x00ff&cfg6[17])<<8) +(0x00ff &  cfg6[16] )));
            ((TextView) findViewById(R.id.tv_SerialNum)).setText(serialNum);
        }
    }

    private void showAbout(){
        VersionInfo.setLength(0);
        //版本号码
        //VersionInfo.append(String.format("Version:%04d,",XE2type));
        //发行日期
        VersionInfo.append(String.format("Date:%02x",(mRecvSysInfo[3]&  0x00ff) -0x55));
        VersionInfo.append(String.format("%02x", (mRecvSysInfo[2]&  0x00ff)  - 0x55));
        VersionInfo.append(String.format("%02x", (mRecvSysInfo[5]&  0x00ff)  - 0x55));
        VersionInfo.append(String.format("%02x, ", (mRecvSysInfo[4]&  0x00ff)  - 0x55));
        //硬件号码
        for (int i = 0; i < 15 ; i++){
            //int info = ((int)mRecvSysInfo[i * 2 + 6]);
            //Log.e("-0x55","(char)"+info);
            VersionInfo.append((char) ((mRecvSysInfo[i * 2 + 6]&  0x00ff)-0x55));
        }
        mTv_versionInfo.setText(VersionInfo);

        //----------------------------------------------------------
        StringBuffer chipPartId =  new StringBuffer();
        switch (mChipPARTID){
            case 0x002c:
                chipPartId.append("TMS320F2801");
                break;
            case 0x0024:
                chipPartId.append("TMS320F2802");
                break;
            case 0x0034:
                chipPartId.append("TMS320F2806");
                break;
            case 0x003c:
                chipPartId.append("TMS320F2808");
                break;
            case 0x0014:
                chipPartId.append("TMS320F28015");
                break;
            case 0x001c:
                chipPartId.append("TMS320F28016");
                break;
            case (byte)0x00bf:
                chipPartId.append("TMS320F28035");
                break;
            case (byte)0x00ab:
                chipPartId.append("TMS320F28030");
                break;
            case (byte)0x00af:
                chipPartId.append("TMS320F28031");
                break;
            default:
                chipPartId.append(String.format("PardID=%04x",mChipPARTID));
        }

        if(XE2type%1000 > 200)		//20120616百位数是1表示24C08的嵌入式代码
        {
            if(mChipPARTID > 0x00a0)		//28030的板子都是24C08，该标识（版本大于100）改为标识电磁铁省电新输出模式（没有PMOS irf9540），
            {
                chipPartId.append(" + no Pmos");
            }
        }
        else if(XE2type%1000 > 100)		//20120616百位数是1表示24C08的嵌入式代码
        {
            chipPartId.append(" + 24C08");
        }
        else
        {
            if(mChipPARTID > 0x00a0)
            {
                chipPartId.append("+  Pmos");		//28030的板子都是24C08，该标识（版本大于100）改为标识电磁铁省电新输出模式（没有PMOS irf9540）
            }
            else
            {
                chipPartId.append("+X5043");
            }
        }
        ((TextView)findViewById(R.id.tv_chipPartId)).setText(chipPartId);

        //----------------------------------------------------------
        String controllerType = String.format("Version:%04d,",XE2type);
        if((XE2type%100) > 53)			//V53以后，53表示平缝，54表示特种机373 781 781CZ
        {
            if((XE2type%2) == 0)
            {
                controllerType +=" 373/781特种机";
            }
            else
            {
                controllerType +="  平缝机型 ";
            }
        }

        //v57以上才判断省电方案
        if((XE2type%100) >= 57)
        {
            if(XE2type > 1000)   //V1057表示电磁铁新省电方案
            {
                controllerType +=" + 新省电方案Cvol";
            }
            else
            {
                controllerType +=" + 老省电方案Cpwm";
            }
        }
        ((TextView)findViewById(R.id.tv_controllerType)).setText(controllerType);
    }

    private void initEditText(){
        mEt[0] = (EditText)findViewById(R.id.et_01);
        mEt[1] = (EditText)findViewById(R.id.et_02);
        mEt[2] = (EditText)findViewById(R.id.et_03);
        mEt[3] = (EditText)findViewById(R.id.et_04);
        mEt[4] = (EditText)findViewById(R.id.et_05);
        mEt[5] = (EditText)findViewById(R.id.et_06);
        mEt[6] = (EditText)findViewById(R.id.et_07);
        mEt[7] = (EditText)findViewById(R.id.et_08);
        mEt[8] = (EditText)findViewById(R.id.et_09);
        mEt[9] = (EditText)findViewById(R.id.et_010);
        mEt[10] = (EditText)findViewById(R.id.et_011);
        mEt[11] = (EditText)findViewById(R.id.et_012);
        mEt[12] = (EditText)findViewById(R.id.et_013);
        mEt[13] = (EditText)findViewById(R.id.et_014);
        mEt[14] = (EditText)findViewById(R.id.et_015);
        mEt[15] = (EditText)findViewById(R.id.et_016);
        mEt[16] = (EditText)findViewById(R.id.et_017);
        mEt[17] = (EditText)findViewById(R.id.et_018);
        mEt[18] = (EditText)findViewById(R.id.et_019);
        mEt[19] = (EditText)findViewById(R.id.et_020);
        mEt[20] = (EditText)findViewById(R.id.et_021);
        mEt[21] = (EditText)findViewById(R.id.et_022);
        mEt[22] = (EditText)findViewById(R.id.et_023);
        mEt[23] = (EditText)findViewById(R.id.et_024);
        mEt[24] = (EditText)findViewById(R.id.et_025);
        mEt[25] = (EditText)findViewById(R.id.et_026);
        mEt[26] = (EditText)findViewById(R.id.et_027);
        mEt[27] = (EditText)findViewById(R.id.et_028);
        mEt[28] = (EditText)findViewById(R.id.et_029);
        mEt[29] = (EditText)findViewById(R.id.et_030);
        mEt[30] = (EditText)findViewById(R.id.et_031);
        mEt[31] = (EditText)findViewById(R.id.et_032);
        //__________________________________________
        mEt[32] = (EditText)findViewById(R.id.et_033);
        mEt[33] = (EditText)findViewById(R.id.et_034);
        mEt[34] = (EditText)findViewById(R.id.et_035);
        mEt[35] = (EditText)findViewById(R.id.et_036);
        mEt[36] = (EditText)findViewById(R.id.et_037);
        mEt[37] = (EditText)findViewById(R.id.et_038);
        mEt[38] = (EditText)findViewById(R.id.et_039);
        mEt[39] = (EditText)findViewById(R.id.et_040);
        mEt[40] = (EditText)findViewById(R.id.et_041);
        mEt[41] = (EditText)findViewById(R.id.et_042);
        mEt[42] = (EditText)findViewById(R.id.et_043);
        mEt[43] = (EditText)findViewById(R.id.et_044);
        mEt[44] = (EditText)findViewById(R.id.et_045);
        mEt[45] = (EditText)findViewById(R.id.et_046);
        mEt[46] = (EditText)findViewById(R.id.et_047);
        mEt[47] = (EditText)findViewById(R.id.et_048);
        mEt[48] = (EditText)findViewById(R.id.et_049);
        mEt[49] = (EditText)findViewById(R.id.et_050);
        mEt[50] = (EditText)findViewById(R.id.et_051);
        mEt[51] = (EditText)findViewById(R.id.et_052);
        mEt[52] = (EditText)findViewById(R.id.et_053);
        mEt[53] = (EditText)findViewById(R.id.et_054);
        mEt[54] = (EditText)findViewById(R.id.et_055);
        mEt[55] = (EditText)findViewById(R.id.et_056);
        mEt[56] = (EditText)findViewById(R.id.et_057);
        mEt[57] = (EditText)findViewById(R.id.et_058);
        mEt[58] = (EditText)findViewById(R.id.et_059);
        mEt[59] = (EditText)findViewById(R.id.et_060);
        mEt[60] = (EditText)findViewById(R.id.et_061);
        mEt[61] = (EditText)findViewById(R.id.et_062);
        mEt[62] = (EditText)findViewById(R.id.et_063);
        mEt[63] = (EditText)findViewById(R.id.et_064);
        //_________________________________________
        mEt[64] = (EditText)findViewById(R.id.et_065);
        mEt[65] = (EditText)findViewById(R.id.et_066);
        mEt[66] = (EditText)findViewById(R.id.et_067);
        mEt[67] = (EditText)findViewById(R.id.et_068);
        mEt[68] = (EditText)findViewById(R.id.et_069);
        mEt[69] = (EditText)findViewById(R.id.et_070);
        mEt[70] = (EditText)findViewById(R.id.et_071);
        mEt[71] = (EditText)findViewById(R.id.et_072);
        mEt[72] = (EditText)findViewById(R.id.et_073);
        mEt[73] = (EditText)findViewById(R.id.et_074);
        mEt[74] = (EditText)findViewById(R.id.et_075);
        mEt[75] = (EditText)findViewById(R.id.et_076);
        mEt[76] = (EditText)findViewById(R.id.et_077);
        mEt[77] = (EditText)findViewById(R.id.et_078);
        mEt[78] = (EditText)findViewById(R.id.et_079);
        mEt[79] = (EditText)findViewById(R.id.et_080);
        mEt[80] = (EditText)findViewById(R.id.et_081);
        mEt[81] = (EditText)findViewById(R.id.et_082);
        mEt[82] = (EditText)findViewById(R.id.et_083);
        mEt[83] = (EditText)findViewById(R.id.et_084);
        mEt[84] = (EditText)findViewById(R.id.et_085);
        mEt[85] = (EditText)findViewById(R.id.et_086);
        mEt[86] = (EditText)findViewById(R.id.et_087);
        mEt[87] = (EditText)findViewById(R.id.et_088);
        mEt[88] = (EditText)findViewById(R.id.et_089);
        mEt[89] = (EditText)findViewById(R.id.et_090);
        mEt[90] = (EditText)findViewById(R.id.et_091);
        mEt[91] = (EditText)findViewById(R.id.et_092);
        mEt[92] = (EditText)findViewById(R.id.et_093);
        mEt[93] = (EditText)findViewById(R.id.et_094);
        mEt[94] = (EditText)findViewById(R.id.et_095);
        mEt[95] = (EditText)findViewById(R.id.et_096);
        //__________________________________________
        mEt[96] = (EditText)findViewById(R.id.et_097);
        mEt[97] = (EditText)findViewById(R.id.et_098);
        mEt[98] = (EditText)findViewById(R.id.et_099);
        mEt[99] = (EditText)findViewById(R.id.et_0100);
        mEt[100] = (EditText)findViewById(R.id.et_0101);
        mEt[101] = (EditText)findViewById(R.id.et_0102);
        mEt[102] = (EditText)findViewById(R.id.et_0103);
        mEt[103] = (EditText)findViewById(R.id.et_0104);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("ReadRawData", "+ ON DESTROY +");

        // Stop the Bluetooth chat services
        if (mBluetoothCommunicateService != null){
            mBluetoothCommunicateService.write(readRam);
            mBluetoothCommunicateService.stop();

        }

    }

    public void onClick_readData(View view){
        if (BluetoothConnectService.getState() != STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
        }
        else {
            if(!etFlag) {
                etFlag =true;
                initEditText();
            }
            mDataLen = 0x60;
            mRecvCount = 0;
            failureSymbol = CMDCODE_READSYSINFO;
            mBluetoothCommunicateService.write(readSysInfo);
            mDownloadCfg.setEnabled(false); mInitCfg.setEnabled(false);
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
                    //String readMessage = Util.byte2Str(readBuf, msg.arg1);
                    //Log.i("READ", "Cfg message received : " + readMessage);
                    try {
                        System.arraycopy(readBuf, 0, mReceived, mRecvCount, msg.arg1);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    mRecvCount+= msg.arg1;
                    Log.i("READ", "message received count: " + mRecvCount + "/" + (mDataLen + 12));
                    if(mRecvCount == (mDataLen+12)){
                        if(CRC16.isCRCChecked(mReceived,mRecvCount)){
                            failureSymbol = 0;
                            mRecvCount = 0;
                            Log.e("Received", "cmd: " + mReceived[4]);
                            switch (mReceived[4]) {
                                case CMDCODE_READSYSINFO:
                                    mRecvSysInfo = Arrays.copyOfRange(mReceived,10,mDataLen+10);
                                    Log.e("SysInfo SHOW", "length: " + mRecvSysInfo.length);
                                    failureSymbol = CMDCODE_READRAM;
                                    mDataLen = 0x10; //下一条指令的读字节数
                                    mBluetoothCommunicateService.write(readRam); //发送下一条指令
                                    break;
                                case CMDCODE_READRAM:
                                    mChipPARTID = mReceived[14];
                                    Log.e("ChipPARTID", "id = " + mChipPARTID);

                                    failureSymbol = READCFG0;
                                    mDataLen = 0x40; //下一条指令的读字节数
                                    mBluetoothCommunicateService.write(readCfg0);//发送下一条指令
                                    break;
                                case CMDCODE_READCFG:
                                    switch (mReceived[6]){
                                        case 0:
                                            mRecvCfg0 = Arrays.copyOfRange(mReceived,10,mDataLen+10);
                                            Log.e("Cfg0 SHOW", "length: " + mRecvCfg0.length);
                                            showDataCfg0(mRecvCfg0);

                                            failureSymbol = READCFG2;
                                            mDataLen = 0x40; //下一条指令的读字节数
                                            mBluetoothCommunicateService.write(readCfg2); //发送下一条指令

                                            break;
                                        case 0x20:
                                            mRecvCfg2 = Arrays.copyOfRange(mReceived,10,mDataLen+10);
                                            Log.e("Cfg2 SHOW", "length: " + mRecvCfg2.length);
                                            showDataCfg2(mRecvCfg2);

                                            failureSymbol = READCFG4;
                                            mDataLen = 0x40; //下一条指令的读字节数
                                            mBluetoothCommunicateService.write(readCfg4); //发送下一条指令
                                            break;
                                        case 0x40:
                                            mRecvCfg4 = Arrays.copyOfRange(mReceived,10,mDataLen+10);
                                            Log.e("Cfg4 SHOW", "length: " + mRecvCfg4.length);
                                            showDataCfg4(mRecvCfg4);
                                            XE2type = ((0x00ff&mRecvCfg4[31])<<8) +  (0x00ff & mRecvCfg4[30]);
                                            showAbout();

                                            failureSymbol = READCFG6;
                                            mDataLen = 0x18; //下一条指令的读字节数
                                            mBluetoothCommunicateService.write(readCfg6); //发送下一条指令
                                            break;
                                        case 0x60:
                                            mRecvCfg6 = Arrays.copyOfRange(mReceived,10,mDataLen+10);
                                            Log.e("Cfg6 SHOW", "length: " + mRecvCfg6.length);
                                            showDataCfg6(mRecvCfg6);
                                            Toast.makeText(getApplicationContext(), "读取完毕！",
                                                    Toast.LENGTH_SHORT).show();
                                            mDataLen = 0x60;
                                            if(!mDownloadCfg.isEnabled()){
                                                mDownloadCfg.setEnabled(true); mInitCfg.setEnabled(true);
                                                mSaveToFile.setEnabled(true); mRestoreFromFile.setEnabled(true);
                                            }
                                            break;
                                    }
                                    break;
                            }
                        }else {
                            mHandler.obtainMessage(MESSAGE_WRITE, failureSymbol, -1, null)
                                    .sendToTarget();
                        }
                    }
                    break;
                case MESSAGE_WRITE:
                    mRecvCount = 0;
                    switch (msg.arg1){  //根据出错的指令重新发送
                        case CMDCODE_READSYSINFO:
                            mDataLen = 0x60;
                            failureSymbol = CMDCODE_READSYSINFO;
                            mBluetoothCommunicateService.write(readSysInfo);
                            break;
                        case CMDCODE_READRAM:
                            mDataLen = 0x10;
                            failureSymbol = CMDCODE_READRAM;
                            mBluetoothCommunicateService.write(readRam);
                            break;
                        case READCFG0:
                            mDataLen = 0x40;
                            failureSymbol = READCFG0;
                            mBluetoothCommunicateService.write(readCfg0);
                            break;
                        case READCFG2:
                            mDataLen = 0x40;
                            failureSymbol = READCFG2;
                            mBluetoothCommunicateService.write(readCfg2);
                            break;
                        case READCFG4:
                            mDataLen = 0x40;
                            failureSymbol = READCFG4;
                            mBluetoothCommunicateService.write(readCfg4);
                            break;
                        case READCFG6:
                            mDataLen = 0x18;
                            failureSymbol = READCFG6;
                            mBluetoothCommunicateService.write(readCfg6);
                            break;
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
