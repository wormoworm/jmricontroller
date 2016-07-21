package uk.tomhomewood.android.jmricontroller.network;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import uk.tomhomewood.android.jmricontroller.Database;
import uk.tomhomewood.android.jmricontroller.network.WiThrottleSocketClient.WithrottleResponseListener;

public class WiThrottleCommandClientBase implements WithrottleResponseListener {
	private final String TAG = "WiThrottleCommandClientBase";

	public static final int COMMAND_DUMMY = 0;
	
	protected Context context;
	protected WiThrottleSocketClient withrottleSocketClient;
	
	protected Database database;
	
	public WiThrottleCommandClientBase(Context context, WiThrottleSocketClient withrottleSocketClient){
		this.context = context;
		this.withrottleSocketClient = withrottleSocketClient;
		this.withrottleSocketClient.addWithrottleResponseListener(this);

		database = new Database(context);
	}
	
	public void release(){
		withrottleSocketClient.removeWithrottleResponseListener(this);
	}
	
	protected void sendCommand(int requestCode, String command, Bundle extras){
		withrottleSocketClient.sendCommand(baseEventListener, requestCode, command, extras);
	}
	
	public void sendDummyCommand(){
		sendCommand(COMMAND_DUMMY, "_", null);
	}

	@Override
	public void responseReceived(String response) {
		//Log.d(TAG, "Response: " + response);
	}

	protected SocketCommandListener baseEventListener = new SocketCommandListener() {
		
		@Override
		public void commandSent(int requestCode, Bundle extras) {
			// TODO So we need to do anything here?
		}
		
		@Override
		public void commandFailed(int requestCode, Bundle extras) {
			// TODO So we need to do anything here?
		}
	};
	
	public interface SocketCommandListener{

		public void commandSent(int requestCode, Bundle extras);
		
		public void commandFailed(int requestCode, Bundle extras);
	}
}