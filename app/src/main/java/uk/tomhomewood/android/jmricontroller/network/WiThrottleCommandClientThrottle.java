package uk.tomhomewood.android.jmricontroller.network;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import uk.tomhomewood.android.jmricontroller.Function;
import uk.tomhomewood.android.jmricontroller.Locomotive;
import uk.tomhomewood.android.jmricontroller.Utils;

public class WiThrottleCommandClientThrottle extends StateCommandClient {
	private String TAG = "CommandClientThrottle";

	public static final int COMMAND_ACQUIRE_LOCOMOTIVE = 30;
	public static final int COMMAND_RELEASE_LOCOMOTIVE = 31;
	public static final int COMMAND_REQUEST_SPEED = 32;
	public static final int COMMAND_REQUEST_DIRECTION = 33;
	public static final int COMMAND_SET_SPEED = 34;
	public static final int COMMAND_SET_DIRECTION = 35;
	public static final int COMMAND_TURN_FUNCTION_ON = 36;
	public static final int COMMAND_TURN_FUNCTION_OFF = 37;

	private static final Integer SPEED_EMERGENCY_STOP = -125;		//The "emergency stop" speed step used in the WiThrottle protocol

	public final String PREFIX_MULTITHROTTLE = "M";
	
	private final String VALID_THROTTLE_CODES = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

	public final String KEY_LOCOMOTIVE = "locomotive";

	private final long STATUS_QUERY_PERIOD = 1000;

	private final float SPEED_CONVERSION_FACTOR = 1.25f;	//Sliders should show speeds 0-100, but DCC speed steps in WiThrottle are (I think?) always 0-125

	private final String DIVIDER = "<;>";

	private List<ThrottleEventListener> throttleEventListeners;
	private Locomotive locomotive;
	private Direction currentLocomotiveDirection;
	private int currentLocomotiveSpeed;
	private boolean releaseBeforeAcquire;
	private boolean statusPollingEnabled;

	private Locomotive locomotivePendingAcquisition;

	private Timer timer;

	private String throttleCode, commandPrefix, acquirePrefix, dropPrefix;
	private int throttleNumber;

	public WiThrottleCommandClientThrottle(Context context, WiThrottleSocketClient wiThrottleSocketClient, int throttleNumber) {
		super(context, wiThrottleSocketClient);
		this.throttleNumber = throttleNumber;
		throttleCode = getThrottleCodeFromNumber(throttleNumber);
		TAG+="_"+throttleCode;
		releaseBeforeAcquire = true;
		statusPollingEnabled = false;
		commandPrefix = PREFIX_MULTITHROTTLE+throttleCode+"A";
		acquirePrefix = PREFIX_MULTITHROTTLE+throttleCode+"+";
		dropPrefix = PREFIX_MULTITHROTTLE+throttleCode+"-";
		currentLocomotiveDirection = Direction.FORWARD;
		currentLocomotiveSpeed = 0;
		throttleEventListeners = new ArrayList<ThrottleEventListener>();
	}
	
	@Override
	public void setReady() {
		super.setReady();
		Iterator<ThrottleEventListener> iterator = throttleEventListeners.iterator();
		while(iterator.hasNext()){
			iterator.next().ready();
		}
	}
	
	public int getThrottleNumber(){
		return throttleNumber;
	}

	public void addThrottleEventListener(ThrottleEventListener throttleEventListener){
		throttleEventListeners.add(throttleEventListener);
	}

	public void removeThrottleEventListener(ThrottleEventListener throttleEventListener){
		throttleEventListeners.remove(throttleEventListener);
	}

	public void setReleaseBeforeAcquire(boolean releaseBeforeAcquire){
		this.releaseBeforeAcquire = releaseBeforeAcquire;
	}

	public void setLocomotive(Locomotive locomotive, boolean forceAcquire){
		if(this.locomotive==null){
			sendAcquireCommand(locomotive);
		}
		else{
			if(!locomotive.equals(this.locomotive)){
				if(releaseBeforeAcquire){
					releaseLocomotive();
				}
				sendAcquireCommand(locomotive);
			}
		}
	}

	private void sendAcquireCommand(Locomotive locomotive) {
		Bundle extras = new Bundle();
		extras.putSerializable(KEY_LOCOMOTIVE, locomotive);
		withrottleSocketClient.sendCommand(baseEventListener, COMMAND_ACQUIRE_LOCOMOTIVE, "M"+throttleCode+"+"+locomotive.getAddress()+"<;>E"+locomotive.getId(), extras);
	}

	private void sendReleaseCommand(Locomotive locomotive) {
		if(locomotive!=null){
			locomotivePendingAcquisition = locomotive;
		}
		withrottleSocketClient.sendCommand(baseEventListener, COMMAND_RELEASE_LOCOMOTIVE, "M"+throttleCode+"-*<;>r", null);
	}

	public void releaseLocomotive(){
		sendReleaseCommand(null);
	}

	public boolean setDirection(Direction direction){
		if(locomotive!=null){
			withrottleSocketClient.sendCommand(baseEventListener, COMMAND_SET_DIRECTION, "M"+throttleCode+"A*<;>R"+direction.textValue, null);
			return true;
		}
		else{
			return false;
		}
	}

	public boolean setSpeed(int speed){
		if(locomotive!=null){
			if(speed>0){
				speed = convertSliderSpeedToRawSpeed(speed);
			}
			withrottleSocketClient.sendCommand(baseEventListener, COMMAND_SET_SPEED, "M"+throttleCode+"A*<;>V"+speed, null);
			return true;
		}
		else{
			return false;
		}
	}
	
	public boolean emergencyStop(){
		if(locomotive!=null){
			withrottleSocketClient.sendCommand(baseEventListener, COMMAND_SET_SPEED, "M"+throttleCode+"A*<;>V"+SPEED_EMERGENCY_STOP, null);
			return true;
		}
		else{
			return false;
		}
	}

	public boolean requestDirection(){
		if(locomotive!=null){
			sendCommand(COMMAND_REQUEST_DIRECTION, "M"+throttleCode+"A*<;>qR", null);
			return true;
		}
		else{
			return false;
		}
	}

	public boolean requestSpeed(){
		if(locomotive!=null){
			sendCommand(COMMAND_REQUEST_SPEED, "M"+throttleCode+"A*<;>qV", null);
			return true;
		}
		else{
			return false;
		}
	}

	public boolean stop(){
		if(locomotive!=null){
			setSpeed(0);
			return true;
		}
		else{
			return false;
		}
	}

	public void turnFunctionOn(Function function) {
		sendCommand(COMMAND_TURN_FUNCTION_ON, "M"+throttleCode+"A*<;>F1"+function.getNumber(), null);
	}

	public void turnFunctionOff(Function function) {
		sendCommand(COMMAND_TURN_FUNCTION_OFF, "M"+throttleCode+"A*<;>F0"+function.getNumber(), null);
	}

	public void setStatusPollingEnabled(boolean statusPollingEnabled) {
		this.statusPollingEnabled = statusPollingEnabled;
	}

	public boolean isStatusPollingEnabled(){
		return statusPollingEnabled;
	}

	public String getThrottleCode(){
		return throttleCode;
	}

	public Locomotive getLocomotive(){
		return locomotive;
	}

	public boolean hasAcquiredLocomotive(){
		return locomotive!=null;
	}

	public Direction getCurrentLocomotiveDirection(){
		return currentLocomotiveDirection;
	}

	public int getCurrentLocomotiveSpeed(){
		return currentLocomotiveSpeed;
	}

	private void handleAcquireResponse(String response) {
		String[] responsePieces = Utils.splitStringByString(response, DIVIDER);
		if(responsePieces!=null && responsePieces.length>0){
			String acquiredLocoAddress = responsePieces[0].substring(3);
			locomotive = database.getLocomotiveByAddress(acquiredLocoAddress);
			if(locomotive!=null){
				requestSpeed();
				requestDirection();
				if(statusPollingEnabled){
					startPollingTask();
				}
				Iterator<ThrottleEventListener> iterator = throttleEventListeners.iterator();
                ThrottleEventListener a;
				while(iterator.hasNext()){
                    a = iterator.next();
                    a.locomotiveAcquired(locomotive);
				}
			}
		}
	}

	private void startPollingTask() {
		timer = new Timer();
		timer.scheduleAtFixedRate(new PollingTask(), 0, STATUS_QUERY_PERIOD);
	}

	private void stopPollingTask(){
		if(timer!=null){
			timer.cancel();
		}
	}

	private void handleDropResponse(String response) {
		locomotive = null;
		if(locomotivePendingAcquisition!=null){
			sendAcquireCommand(locomotivePendingAcquisition);
			locomotivePendingAcquisition = null;
		}
		else{
			Iterator<ThrottleEventListener> iterator = throttleEventListeners.iterator();
			while(iterator.hasNext()){
				iterator.next().locomotiveReleased();
			}
			stopPollingTask();
		}
	}

	private void handleDirectionResponse(String response) {
		if(response!=null && response.length()>1){
			Direction newDirection = Direction.getTypeFromValue(response.substring(1));
			Direction previousDirection = currentLocomotiveDirection;
			if(newDirection!=null && (currentLocomotiveDirection==null || newDirection!=currentLocomotiveDirection)){		//Direction has changed or current direction is not set
				currentLocomotiveDirection = newDirection;
				if(locomotive!=null){
					Iterator<ThrottleEventListener> iterator = throttleEventListeners.iterator();
					while(iterator.hasNext()){
						iterator.next().locomotiveDirectionChanged(locomotive, currentLocomotiveDirection, previousDirection);
					}
				}
			}
		}
	}

	private void handleSpeedResponse(String response) {
		if(response!=null && response.length()>1){
			try{
				Integer newLocomotiveSpeed = Integer.parseInt(response.substring(1));
				if(newLocomotiveSpeed==SPEED_EMERGENCY_STOP){
					if(locomotive!=null){
						Iterator<ThrottleEventListener> iterator = throttleEventListeners.iterator();
						while(iterator.hasNext()){
							iterator.next().locomotiveEmergencyStop(locomotive);
						}
					}
				}
				else {
					newLocomotiveSpeed = convertRawSpeedToSliderSpeed(newLocomotiveSpeed);
					if(newLocomotiveSpeed!=currentLocomotiveSpeed){			//Speed has changed
						currentLocomotiveSpeed = newLocomotiveSpeed;
						if(locomotive!=null){
							Iterator<ThrottleEventListener> iterator = throttleEventListeners.iterator();
							while(iterator.hasNext()){
								iterator.next().locomotiveSpeedChanged(locomotive, currentLocomotiveSpeed);
							}
						}
					}
				}
			}
			catch(NumberFormatException e){}
		}
	}

	private int convertRawSpeedToSliderSpeed(int rawSpeed) {
		int convertedSpeed = Math.round((float) rawSpeed / SPEED_CONVERSION_FACTOR);
		if(convertedSpeed<0){
			convertedSpeed = 0;
		}
		else if(convertedSpeed>100){
			convertedSpeed = 100;
		}
		return convertedSpeed;
	}

	private int convertSliderSpeedToRawSpeed(int sliderSpeed){
		return Math.round((float) sliderSpeed * SPEED_CONVERSION_FACTOR);
	}

	private void handleFunctionResponse(String response) {
		if(response!=null && response.length()>1){
			Iterator<ThrottleEventListener> iterator = throttleEventListeners.iterator();
			try{
				Integer functionNumber = Integer.parseInt(response.substring(2));
				//Log.d(TAG, "F: "+functionNumber+", "+response.charAt(1));
				switch(response.charAt(1)){
				case '0':
					while(iterator.hasNext()){
						iterator.next().functionTurnedOff(functionNumber);
					}
					break;
				case '1':
					while(iterator.hasNext()){
						iterator.next().functionTurnedOn(functionNumber);
					}
					break;
				default:
					break;
				}
			}
			catch(Exception e){}
		}
	}

	@Override
	public void responseReceived(String response) {
		if(response.startsWith(acquirePrefix)){
			handleAcquireResponse(response);
		}
		else if(response.startsWith(dropPrefix)){
			handleDropResponse(response);
		}
		else if(response.startsWith(commandPrefix)){
			String[] responsePieces = Utils.splitStringByString(response, DIVIDER);
			//Sanity check the loco address, just to check it is for this throttle
			if(responsePieces!=null && locomotive!=null && responsePieces.length>0 && responsePieces[0].substring(3).equals(locomotive.getAddress())){
				if(responsePieces.length>1){
					switch(responsePieces[1].charAt(0)){
					case 'R':			//Direction response
						handleDirectionResponse(responsePieces[1]);
						break;
					case 'V':			//Speed response
						handleSpeedResponse(responsePieces[1]);
						break;
					case 'F':			//Function response
						handleFunctionResponse(responsePieces[1]);
						break;
					default:
						break;
					}
				}
			}
		}
		super.responseReceived(response);
	}

	public enum Direction{
		FORWARD("1"),
		REVERSE("0");

		public final String textValue;

		Direction(String textValue){
			this.textValue = textValue;
		}

		public static Direction getTypeFromValue(String value){
			Direction returnType = null;
			if(value!=null){
				if(value.equals(FORWARD.textValue)){
					returnType = FORWARD;
				}
				else if(value.equals(REVERSE.textValue)){
					returnType = REVERSE;
				}
			}
			return returnType;
		}
	}

	private class PollingTask extends TimerTask {
		@Override
		public void run() {
			if(statusPollingEnabled){
				requestSpeed();
				requestDirection();
			}
		}
	};

	private String getThrottleCodeFromNumber(int throttleNumber) {
		String throttleCode;
		try{
			throttleCode = VALID_THROTTLE_CODES.substring(throttleNumber, throttleNumber + 1);
		}
		catch(IndexOutOfBoundsException e){
			throttleCode = "A";					//Default to code 'A'
		}
		return throttleCode;
	}

	public interface ThrottleEventListener {
		
		public void ready();

		public void locomotiveAcquired(Locomotive locomotive);

		public void locomotiveReleased();

		public void locomotiveSpeedChanged(Locomotive locomotive, int speed);

		public void locomotiveDirectionChanged(Locomotive locomotive, Direction direction, Direction previousDirection);
		
		public void locomotiveEmergencyStop(Locomotive locomotive);

		public void functionTurnedOn(int functionNumber);

		public void functionTurnedOff(int functionNumber);
	}
}