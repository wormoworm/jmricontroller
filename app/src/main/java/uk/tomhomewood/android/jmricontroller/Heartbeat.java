package uk.tomhomewood.android.jmricontroller;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.util.Log;

import uk.tomhomewood.android.jmricontroller.network.WiThrottleCommandClientBase;
import uk.tomhomewood.android.jmricontroller.network.WiThrottleCommandClientCore;
import uk.tomhomewood.android.jmricontroller.network.WiThrottleCommandClientCore.CoreEventListener;
import uk.tomhomewood.android.jmricontroller.network.WiThrottleCommandClientCore.PowerState;
import uk.tomhomewood.android.jmricontroller.network.WiThrottleSocketClient;

public class Heartbeat implements CoreEventListener {
	private final String TAG = "Heartbeat";
	
	private WiThrottleCommandClientBase commandClient;
	private Timer timer;
	
	private int interval;
	private boolean isRunning;
	
	public Heartbeat(Context context, WiThrottleSocketClient wiThrottleSocketClient){
		commandClient = new WiThrottleCommandClientCore(context, wiThrottleSocketClient, this);
		isRunning = false;
	}
	
	public void start(int heartbeatInterval){
		interval = heartbeatInterval;
		timer = new Timer();
		try{
			timer.scheduleAtFixedRate(heartbeatPulse, 0, interval);
		}
		catch(IllegalStateException e){
			Log.e(TAG, "Error starting heartbeat: "+e.toString());
		}
		isRunning = true;
	}
	
	public void stop(){
		timer.cancel();
		isRunning = false;
	}
	
	public boolean isRunning(){
		return isRunning;
	}
	
	public int getInterval(){
		return interval;
	}
	
	private TimerTask heartbeatPulse = new TimerTask() {
		@Override
		public void run() {
			commandClient.sendDummyCommand();
		}
	};

	@Override
	public void layoutPowerStateChanged(PowerState newPowerState) {
	}
}