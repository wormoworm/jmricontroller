package uk.tomhomewood.android.jmricontroller.network;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import uk.tomhomewood.android.jmricontroller.Turnout;

public class WiThrottleCommandClientTurnouts extends StateCommandClient {
	private final String TAG = "WiThrottleCommandClientTurnouts";
	
	public static final int COMMAND_THROW_TURNOUT = 20;
	public static final int COMMAND_CLOSE_TURNOUT = 21;
	public static final int COMMAND_TOGGLE_TURNOUT = 22;

	public static final String PREFIX_TURNOUT_LIST = "PTL";
	public static final String PREFIX_TURNOUT_CHANGE = "PTA";
	
	private List<TurnoutEventListener> turnoutEventListeners;

	public WiThrottleCommandClientTurnouts(Context context, WiThrottleSocketClient wiThrottleSocketClient) {
		super(context, wiThrottleSocketClient);
		turnoutEventListeners = new ArrayList<TurnoutEventListener>();
	}
	
	@Override
	public void setReady() {
		super.setReady();
		Iterator<TurnoutEventListener> iterator = turnoutEventListeners.iterator();
		while(iterator.hasNext()){
			iterator.next().ready();
		}
	}

	public void addTurnoutEventListener(TurnoutEventListener turnoutEventListener){
		turnoutEventListeners.add(turnoutEventListener);
	}
	
	public void removeTurnoutEventListener(TurnoutEventListener turnoutEventListener){
		turnoutEventListeners.remove(turnoutEventListener);
	}
	
	public void throwTurnout(Turnout turnout, Bundle extras){
		withrottleSocketClient.sendCommand(baseEventListener, COMMAND_THROW_TURNOUT, "PTAT"+turnout.getAddress(), extras);
	}
	
	public void closeTurnout(Turnout turnout, Bundle extras){
		withrottleSocketClient.sendCommand(baseEventListener, COMMAND_CLOSE_TURNOUT, "PTAC"+turnout.getAddress(), extras);
	}
	
	public void toggleTurnout(Turnout turnout, Bundle extras){
		withrottleSocketClient.sendCommand(baseEventListener, COMMAND_TOGGLE_TURNOUT, "PTA2"+turnout.getAddress(), extras);
	}
	
	@Override
	public void responseReceived(String response) {
		if(response.startsWith(PREFIX_TURNOUT_CHANGE)){
			handleTurnoutChanged(response);
		}
		super.responseReceived(response);
	}

	private void handleTurnoutChanged(String response) {
		if(response!=null){
			try{
				int turnoutState = Integer.parseInt(response.substring(3,4));
				String turnoutAddress = response.substring(4);
				if(!turnoutAddress.isEmpty()){
					Turnout turnoutChanged = database.getTurnoutByAddress(turnoutAddress);
					if(turnoutChanged!=null){
						turnoutChanged.setState(turnoutState);
						Iterator<TurnoutEventListener> iterator = turnoutEventListeners.iterator();
						while(iterator.hasNext()){
							iterator.next().turnoutStateChanged(turnoutChanged);
						}
					}
				}
			}
			catch(Exception e){
				Log.e(TAG, "Error parsing turnout state response: "+e.toString());
			}
		}
	}
	
	public interface TurnoutEventListener {
		
		public void ready();
		
		public void turnoutStateChanged(Turnout turnout);
	}
}