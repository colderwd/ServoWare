package zju.cse.servoware;


import android.graphics.Typeface;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;


public class Util {

    public static String byte2Str(byte[] buf, int len) {
        StringBuffer out = new StringBuffer();
        for (int i = 0; i < len; i++) {
            out.append(String.format("%02x, ", buf[i]));
        }
        return out.toString();
    }


    public static byte[] rdDatCmmandLine(int startAddress, int length)
    {
        byte pCommandLine[] = new byte[12];
        pCommandLine[0] = 2;
        pCommandLine[1] = (byte)0x55;
        pCommandLine[2] = 8;
        pCommandLine[3] = 0;
        pCommandLine[4] = 1;	//命令码
        pCommandLine[5] = (byte)(startAddress>>16);
        pCommandLine[6] = (byte)(startAddress&0xff);
        pCommandLine[7] = (byte)((startAddress&0xff00)>>8);
        pCommandLine[8] = (byte)(length&0xff);
        pCommandLine[9] = (byte)((length&0xff00)>>8);
        short k = CRC16.CRC_16(pCommandLine, 0,10);
        pCommandLine[10] = (byte)(k&0xff);
        pCommandLine[11] = (byte)((k&0xff00)>>8);
        return pCommandLine;
    }

    public static byte[] wrCfgCommandLine(byte[] data,int StartItem)
    {   int nLen = data.length;
        int nStaticLen = 12;
        if(StartItem == 0x60) nLen = 16;
        byte pCommandLine[] = new byte[nStaticLen+nLen];
        pCommandLine[0] = 2;
        pCommandLine[1] = (byte)0x55;
        pCommandLine[2] = (byte)((nLen+nStaticLen-4)&0xff);
        pCommandLine[3] = (byte)(((nLen+nStaticLen-4)&0xff00)>>8);
        pCommandLine[4] = 14;							//命令码
        pCommandLine[5] = 0;									//定值0
        pCommandLine[6] = (byte)(StartItem&0xff);				//组态号
        pCommandLine[7] = (byte)(nLen&0xff);					//数量
        pCommandLine[8] = 0;				//组态号
        pCommandLine[9] = 0;
        System.arraycopy(data,0,pCommandLine,nStaticLen-2,nLen);
        short k = CRC16.CRC_16(pCommandLine, 0,(nStaticLen - 2) + nLen);
        pCommandLine[nLen + nStaticLen - 2] = (byte)(k&0xff);
        pCommandLine[nLen + nStaticLen - 1] = (byte)((k&0xff00)>>8);
        return pCommandLine;
    }

    public static byte[] specialFuncCommandLine(byte cmd, byte type){
        byte pCommandLine[] = new byte[8];
        pCommandLine[0] = 2;
        pCommandLine[1] = (byte)0x55;
        pCommandLine[2] = 4;
        pCommandLine[3] = 0;
        pCommandLine[4] = cmd;	//命令码
        pCommandLine[5] = type;
        short k = CRC16.CRC_16(pCommandLine, 0,6);
        pCommandLine[6] = (byte)(k&0xff);
        pCommandLine[7] = (byte)((k&0xff00)>>8);
        return pCommandLine;
    }


}
