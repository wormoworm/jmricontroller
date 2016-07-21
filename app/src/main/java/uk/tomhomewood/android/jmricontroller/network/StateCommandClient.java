package uk.tomhomewood.android.jmricontroller.network;

import android.content.Context;

public class StateCommandClient extends WiThrottleCommandClientBase {

	private boolean isReady;

	public StateCommandClient(Context context, WiThrottleSocketClient withrottleSocketClient) {
		super(context, withrottleSocketClient);
	}
	
	public void setReady(){
		isReady = true;
	}
	
	public boolean isReady(){
		return isReady;
	}
}