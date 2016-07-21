package uk.tomhomewood.android.jmricontroller.network;

import android.content.Context;
import android.os.Bundle;
import uk.tomhomewood.android.jmricontroller.Database;

public class WiThrottleCommandClientCore extends WiThrottleCommandClientBase {
//	private final String TAG = "WiThrottleCommandClientCore";

	public static final int COMMAND_DUMMY = 0;
	public static final int COMMAND_SEND_THROTTLE_NAME = 1;
	public static final int COMMAND_DISCONNECT_FROM_SERVER = 2;
	public static final int COMMAND_DISABLE_HEARTBEAT = 3;
	public static final int COMMAND_LAYOUT_POWER_ON = 10;
	public static final int COMMAND_LAYOUT_POWER_OFF = 11;

	public static final int LAYOUT_POWER_STATE_OFF = 0;
	public static final int LAYOUT_POWER_STATE_ON = 1;
	public static final int LAYOUT_POWER_STATE_UNKOWN = 2;
	
	private final String RESPONSE_PREFIX_POWER = "PPA";
	
	protected CoreEventListener coreEventListener;
	
	protected Database database;
	
	private PowerState layoutPowerState;
	
	public WiThrottleCommandClientCore(Context context, WiThrottleSocketClient withrottleSocketClient, CoreEventListener coreEventListener){
		super(context, withrottleSocketClient);
		this.coreEventListener = coreEventListener;
		this.withrottleSocketClient.addWithrottleResponseListener(this);

		database = new Database(context);
		
		layoutPowerState = PowerState.UNKNOWN;
	}
	
	public void sendThrottleName(String throttleName, Boolean sendHWID, Bundle extras) {
		sendCommand(COMMAND_SEND_THROTTLE_NAME, "N" + throttleName, extras);  //Send throttle name
		if(sendHWID){
			sendCommand(COMMAND_SEND_THROTTLE_NAME, "HU" + throttleName, extras);  //also send throttle name as the UDID
		}
	}
	
	public void disconnectFromServer(boolean alsoDisableHeartbeat, Bundle extras){
		sendCommand(COMMAND_DISCONNECT_FROM_SERVER, "Q", extras);
		if(alsoDisableHeartbeat){
			sendCommand(COMMAND_DISABLE_HEARTBEAT, "*-", extras);
		}
	}
	
	public void turnLayoutPowerOn(Bundle extras){
		sendCommand(COMMAND_LAYOUT_POWER_ON, "PPA"+PowerState.ON.textValue, extras);
	}
	
	public void turnLayoutPowerOff(Bundle extras){
		sendCommand(COMMAND_LAYOUT_POWER_OFF, "PPA"+PowerState.OFF.textValue, extras);
	}
	
	public PowerState getLayoutPowerState(){
		return layoutPowerState;
	}

	@Override
	public void responseReceived(String response) {
		if(response.startsWith(RESPONSE_PREFIX_POWER)){
			PowerState newLayoutPowerState = PowerState.getTypeFromValue(response.substring(3, 4));
			if(coreEventListener!=null && newLayoutPowerState!=layoutPowerState){
				layoutPowerState = newLayoutPowerState;
				coreEventListener.layoutPowerStateChanged(layoutPowerState);
			}
		}
	}
	
	public enum PowerState{
		OFF("0"),
		ON("1"),
		UNKNOWN("2");

		public final String textValue;

		PowerState(String textValue){
			this.textValue = textValue;
		}

		public static PowerState getTypeFromValue(String value){
			PowerState returnType = UNKNOWN;
			if(value!=null){
				if(value.equals(ON.textValue)){
					returnType = ON;
				}
				else if(value.equals(OFF.textValue)){
					returnType = OFF;
				}
			}
			return returnType;
		}
	}
	
	public interface CoreEventListener{

		public void layoutPowerStateChanged(PowerState newPowerState);
	}
}