package zju.cse.servoware;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class ReadRawDataActivity extends Activity implements Constant{

    private TextView mTv_showData,mTv_xUnit;
    private EditText mEt_readFrom, mEt_readCount;
    private RadioButton mRb_showContent, mRb_showAll;
    private Spinner mSpinner_bytesCount, mSpinner_unit;
    private byte cmdEnd[]={(byte)0x02,(byte)0x55,(byte)0x08,(byte)0x00,(byte)0x01,(byte)0x00,(byte)0x80,(byte)0x08,(byte)0x10,(byte)0x00,(byte)0x61,(byte)0xEF},
                mReceived[]=new byte[124], mRecvdData[] = new byte[2100];
    private int mColumn = 16, mByteoffset = 1, mAddress, mReadLen,mRecvdCount = 0,mPos=0,mTraceData[]=new int[1000],typeOFdata;
    private BluetoothCommunicateService mBluetoothCommunicateService = null;
    private LinearLayout ll_trace;
    private TraceView mTraceview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("ReadRawData", "+ ON CREATE +");
        setContentView(R.layout.activity_read_raw_data);
        initViews();
        mBluetoothCommunicateService = new BluetoothCommunicateService(mHandler);

    }

    private void initViews(){
        mTv_showData = (TextView)findViewById(R.id.showData);
        mTv_showData.setMovementMethod(ScrollingMovementMethod.getInstance());
        mTv_showData.setFocusable(true);
        mTv_showData.setFocusableInTouchMode(true);
        mTv_showData.requestFocus();
        mTv_showData.requestFocusFromTouch();

        mTv_xUnit = (TextView)findViewById(R.id.tv_xUint);
        ll_trace = ((LinearLayout)findViewById(R.id.lv_trace));
        mTraceview = new TraceView(this);
        ll_trace.addView(mTraceview);

        mEt_readFrom = (EditText)findViewById(R.id.et_read_from);
        mEt_readCount = (EditText)findViewById(R.id.et_read_count);

        ArrayAdapter<CharSequence> mBytesCountSpinnerAdapter, mUintSpinnerAdapter;
        mSpinner_bytesCount = (Spinner) findViewById(R.id.spinner_bytesCount);
        mBytesCountSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.bytesCount_list, android.R.layout.simple_spinner_item);
        mBytesCountSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner_bytesCount.setAdapter(mBytesCountSpinnerAdapter);
        mSpinner_bytesCount.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int arg2, long arg3) {
                mColumn = Integer.valueOf(mSpinner_bytesCount.getSelectedItem().toString());
                if (mTv_showData.getVisibility()==View.VISIBLE) {
                    mTv_showData.setText(setShowData(mReceived, mColumn, mByteoffset, mAddress, mPos, mReadLen));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {

            }

        });

        mSpinner_unit = (Spinner) findViewById(R.id.spinner_unit);
        mUintSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.unit_list, android.R.layout.simple_spinner_item);
        mUintSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner_unit.setAdapter(mUintSpinnerAdapter);
        mSpinner_unit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int arg2, long arg3) {
                mByteoffset = Integer.valueOf(mSpinner_unit.getSelectedItem().toString());
                if (mTv_showData.getVisibility()==View.VISIBLE) {
                    mTv_showData.setText(setShowData(mReceived, mColumn, mByteoffset, mAddress, mPos, mReadLen));
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {

            }

        });

        mRb_showContent = (RadioButton) findViewById(R.id.rb_showContent);
        mRb_showContent.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mPos = 10;
                    if (mTv_showData.getVisibility()==View.VISIBLE) {
                        mTv_showData.setText(setShowData(mReceived, mColumn, mByteoffset, mAddress, mPos, mReadLen));
                    }
                }
            }
        });
        mRb_showAll = (RadioButton)findViewById(R.id.rb_showAll);
        mRb_showAll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mPos = 0;
                    if (mTv_showData.getVisibility()==View.VISIBLE) {
                        mTv_showData.setText(setShowData(mReceived, mColumn, mByteoffset, mAddress, mPos, mReadLen));
                    }
                }
            }
        });
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
    }

    public void onClick_readTrace(View view){
        mBluetoothCommunicateService.write(Util.specialFuncCommandLine(CMDCODE_SysCommand, (byte) 14));//必须停止电机并清除故障状态（OnRstFaultRecord() ）
        Toast.makeText(getApplicationContext(), "响应曲线数据读取中...", Toast.LENGTH_LONG).show();
        try {
            Thread.sleep(50);	//等待电机停止
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mAddress = 0; mRecvdCount = 0; mReadLen = 100;
        mBluetoothCommunicateService.write(Util.rdDatCmmandLine(mAddress, 100));
        mTv_showData.setVisibility(View.GONE);
        ll_trace.setVisibility(View.VISIBLE);
    }

    public void onClick_readData(View view){
        sendCmd();
        mTv_showData.setVisibility(View.VISIBLE);
        ll_trace.setVisibility(View.GONE);
    }

    private void sendCmd(){
        //清空原数据
        mRecvdCount = 0;
        //读取命令参数
        String sReadFrom, sReadCount;
        if(!(sReadFrom = mEt_readFrom.getText().toString()).equals("")){
            mAddress = Integer.valueOf(sReadFrom, 16);
        }else {
            mAddress = 0x0A80;
        }
        if(!(sReadCount = mEt_readCount.getText().toString()).equals("")){
            mReadLen = Integer.valueOf(sReadCount, 16);
            if(mReadLen >= 0x70){
                mReadLen = 0x70;
            }
            if(mReadLen == 100){
                mReadLen= 0x60;  //防止与读动态曲线冲突
            }
        }else {
            mReadLen= 0x60;
        }
        mBluetoothCommunicateService.write(Util.rdDatCmmandLine(mAddress,mReadLen));
    }

    private String setShowData(byte[] received, int column, int byteoffset, int address,int pos, int len)
    {
        if(pos==0) len+=12;//显示全部
        StringBuffer display  = new StringBuffer();
        int i;
        display.append(String.format("0x%04x: ", address));
        if(column == 0)
            column = 1;
        for(i=0;i<len;i++)
        {
            if((i%column==0)&&(i!=0)){
                display.append("\n");
                display.append(String.format("0x%04x: ", address+i/2));
            }
            if(byteoffset == 0 ||(i % byteoffset != 0)&&(i!=0))
            {
                display.append(String.format("%02x", received[i+pos]));
            }
            else
            {
                display.append(String.format(" %02x", received[i+pos]));
            }
        }

        return display.toString();
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
                    try {
                        System.arraycopy(readBuf, 0, mReceived, mRecvdCount, msg.arg1);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    mRecvdCount+= msg.arg1;
                    Log.i("READ", "message received count: " + mRecvdCount + "/" + (mReadLen + 12));
                    if(mRecvdCount == (mReadLen+12)){
                        //Log.e("SHOW", "length: " + mReceived.length + " : " + Util.byte2Str(mReceived, mReceived.length));
                        if(CRC16.isCRCChecked(mReceived, mRecvdCount)){
                            switch (mReceived[8]){
                                case 100:
                                    Log.e("SHOW", "length: " + mReceived.length + " traceData" );
                                    try {
                                        Thread.sleep(30);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    System.arraycopy(mReceived,10,mRecvdData,mAddress*2,100);
                                    if((mAddress+=50)<1024){
                                        mRecvdCount = 0;
                                        mBluetoothCommunicateService.write(Util.rdDatCmmandLine(mAddress,100));
                                    }else {
                                        typeOFdata = ((0x00ff & mRecvdData[2013])<<8) +(0x00ff & mRecvdData[2012]);
                                        Log.e("invalidate", "paint");
                                        byte2int(mRecvdData, mTraceData);
                                        mTraceview.invalidate();
                                    }
                                    break;
                                default:
                                    Toast.makeText(getApplicationContext(), "读取数据成功！", Toast.LENGTH_SHORT).show();
                                    if(mRb_showAll.isChecked()){
                                        mPos = 0;
                                    }else{
                                        mPos = 10;
                                    }
                                    mTv_showData.setText(setShowData(mReceived, mColumn, mByteoffset, mAddress, mPos, mReadLen));
                                    break;
                            }
                        }else {
                            Log.e("CRCcheck", "failed "+ mReceived[8]);
                            switch (mReceived[8]){
                                case 100:
                                    mRecvdCount = 0;
                                    mBluetoothCommunicateService.write(Util.rdDatCmmandLine(mAddress,100));
                                    break;
                                default:
                                    sendCmd();
                                    break;
                            }
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

    private void byte2int(byte[] data, int[] paras){
        double co_rata = 1.0;
        if(((typeOFdata>>4)&0x0f)==0) typeOFdata+=0xa0;
        mTv_xUnit.setText(String.format("Time Unit:%dms", (typeOFdata >> 4) & 0x0f));
        switch(typeOFdata&0x0f){
            case 0:
                ((TextView)findViewById(R.id.tv_dataType)).setText("Motor SPEED") ;
                ((TextView)findViewById(R.id.tv_yUnit)).setText("rpm") ;
                co_rata = 1.0;
                break;
            case 1:
                ((TextView)findViewById(R.id.tv_dataType)).setText("PWM OUT") ;
                ((TextView)findViewById(R.id.tv_yUnit)).setText("×0.01%") ;
                co_rata = 10000.0/4096.0;
                break;
            case 2:
                co_rata = 1.0;
                ((TextView)findViewById(R.id.tv_dataType)).setText("Speed_SV") ;
                ((TextView)findViewById(R.id.tv_yUnit)).setText("rpm") ;
                break;
            case 3:
                ((TextView)findViewById(R.id.tv_dataType)).setText("SpeedCommandVelocity") ;
                ((TextView)findViewById(R.id.tv_yUnit)).setText("rpm") ;
                break;
            case 4:
                ((TextView)findViewById(R.id.tv_dataType)).setText("脚踏行程") ;
                ((TextView)findViewById(R.id.tv_yUnit)).setText("%") ;
                co_rata = (float)100.00/(float)0x3f5;
                break;
            case 5:	((TextView)findViewById(R.id.tv_dataType)).setText("--Ei速度偏差--") ;
                ((TextView)findViewById(R.id.tv_yUnit)).setText("rpm") ;
                break;
            case 6:
                ((TextView)findViewById(R.id.tv_dataType)).setText("X_Angle一个相位内相对位置") ;
                ((TextView)findViewById(R.id.tv_yUnit)).setText("0.25度") ;
                break;
            case 7:
                ((TextView)findViewById(R.id.tv_dataType)).setText("--SewRotateCNT机头位置--") ;
                ((TextView)findViewById(R.id.tv_yUnit)).setText("0.25度") ;
                break;
            case 8:
                ((TextView)findViewById(R.id.tv_dataType)).setText("--电机编码器输出，位置--") ;
                ((TextView)findViewById(R.id.tv_yUnit)).setText("0.25度") ;
                break;
            default:
                ((TextView)findViewById(R.id.tv_dataType)).setText("--未知变量--") ;
                ((TextView)findViewById(R.id.tv_yUnit)).setText("    ") ;
                break;
        }
        int ptrBuf = 1+((0x00ff & mRecvdData[2001])<<8) +(0x00ff & mRecvdData[2000]);	//记录数据指针
        if(ptrBuf > 1000) ptrBuf = 0;

        int i=ptrBuf, j=0;
        for(; i<1000; i++)
        {
            paras[j++] = (short)( (((0x00ff & data[2*i+1])<<8) +(0x00ff & data[2*i])) *co_rata);
        }
        for( i=0; i<ptrBuf && j<1000; i++)
        {
            paras[j++] = (short)( (((0x00ff & data[2*i+1])<<8) +(0x00ff & data[2*i])) *co_rata);
        }
    }

    class TraceView extends View{
        private Paint paint1,paint2;

        public TraceView(Context context){
            super(context);
            setBackgroundColor(Color.rgb(192,192,192));
            paint1 = new Paint();
            paint1.setColor(Color.BLACK);
            paint2 = new Paint();
            paint2.setColor(Color.RED);
            paint2.setStrokeWidth(3);
        }

        /*public boolean onTouchEvent(MotionEvent e){
            int x0=(int)e.getX();
            int y0=(int)e.getY();
            invalidate();
            return super.onTouchEvent(e);
        }*/

        protected void onDraw(Canvas canvas){
            int width = ll_trace.getWidth();
            int height = ll_trace.getHeight()-mTv_xUnit.getHeight();

            // 确定速度最大值, 最小值(化为STEP的整数倍)
            int STEP = 200; // 200为一个速度步长
            int nMaxData, nMinData; // 速度的最大值, 最小值

            int i;
            for (i=1, nMaxData=mTraceData[0], nMinData = mTraceData[0]; i<1000; i++)
            {
                if (nMaxData < mTraceData[i])
                {
                    nMaxData = mTraceData[i];
                }
                if (nMinData > mTraceData[i])
                {
                    nMinData = mTraceData[i];
                }
            }
            if(nMaxData<200) nMaxData=200;
            STEP = (nMaxData-nMinData)/20;
            if(((typeOFdata>>4)&0x0f)==0) typeOFdata+=0xa0;
            //m_Xunit.Format(_T("Time Unit:%dms"), (typeOFdata>>4)&0x0f);
            switch(typeOFdata&0x0f)
            {
                case 0:
                    nMaxData = ((nMaxData+200)/200) *200;	//取200的整数，进位处理
                    STEP = (nMaxData-nMinData)/20;
                    if(STEP>100)STEP = ((STEP+50)/100) *100;
                    break;
                case 1:
                    nMaxData = 10000;
                    nMinData = -10000;
                    break;
                case 4:
                    nMaxData = 100;
                    nMinData = -100;
                    break;
            }

            if (nMaxData <= 0){
                nMaxData = 0;
            }
            else if (nMaxData%STEP != 0){
                nMaxData = (nMaxData/STEP + 1) * STEP;
            }
            if (nMinData >= 0){
                nMinData = 0;
            }
            else if (nMinData%STEP != 0){
                nMinData = (Math.abs(nMinData)/STEP + 1) * -STEP;
            }	// 如果nMaxData, nMinData先前为0, 则判断后值依然为0
            int nDataHeight = (nMaxData - nMinData); // 数据跨度
            int nData;
            float nYPix;
            if (nDataHeight != 0){
                for (nData = nMinData; nData <= nMaxData; nData += STEP){
                    nYPix = (height-20)*((nData-nMinData)/((float)nDataHeight))+10;
                    canvas.drawLines(new float[]{10,nYPix, width-10, nYPix},paint1);
                    canvas.drawText(Integer.toString(nMaxData+nMinData-nData),0,nYPix,paint1);
                }
            }
            else{
                canvas.drawText(Integer.toString(0),0,height,paint1);
            }
            for( i=0;i<21;i++){
                canvas.drawLines(new float[]{10+(width-20)/20*i,10,10+(width-20)/20*i,height-10},paint1);
                canvas.drawText(Integer.toString(i*50),10+(width-20)/20*i,height,paint1);
            }

            Log.e("mTraceData[900]",mTraceData[900]+"");
            for (i=0; i<999; i++)
            {
                canvas.drawLines(new float[]{(10+i*(width-20)/1000),height-(height-20)*((float)mTraceData[i])/nDataHeight-10+(nMinData<0?(height-20)*((float)nMinData)/nDataHeight:0),
                        10+((i+1)*(width-20)/1000), height-(height-20)*((float)mTraceData[i+1])/nDataHeight-10+(nMinData<0?(height-20)*((float)nMinData)/nDataHeight:0)},paint2);
            }


        }


    }

}
