package zju.cse.servoware;

import android.os.Environment;

public interface Constant { 

	//命令码
	byte CMDCODE_READRAM = 1;
	byte CMDCODE_WRITERAM = 2;
	byte CMDCODE_READREG = 3;
	byte CMDCODE_WRITEROM = 6;
	byte CMDCODE_READROM = 7;
	byte CMDCODE_SysCommand	= 8;

	byte CMDCODE_TestData = 10;
	byte CMDCODE_READSYSINFO = 11;
	byte CMDCODE_WRITECFG = 14;
	byte CMDCODE_READCFG = 15;

	int READCFG0 = 150;
	int READCFG2 = 152;
	int READCFG4 = 154;
	int READCFG6 = 156;

	// Message types sent from the BluetoothChatService Handler
	int MESSAGE_STATE_CHANGE = 1;
	int MESSAGE_READ = 2;
	int MESSAGE_WRITE = 3;
	int MESSAGE_DEVICE_NAME = 4;
	int MESSAGE_TOAST = 5;
	int MESSAGE_WS = 7;

	// Key names received from the BluetoothChatService Handler
	String DEVICE_NAME = "device_name";
	String TOAST = "toast";

	// Constants that indicate the current connection state
	int STATE_NONE = 0;       // we're doing nothing
	int STATE_LISTEN = 1;     // now listening for incoming connections
	int STATE_CONNECTING = 2; // now initiating an outgoing connection
	int STATE_CONNECTED = 3;  // now connected to a remote device


		
 
}
