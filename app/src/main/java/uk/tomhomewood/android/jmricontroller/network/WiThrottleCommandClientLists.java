package uk.tomhomewood.android.jmricontroller.network;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import uk.tomhomewood.android.jmricontroller.Locomotive;
import uk.tomhomewood.android.jmricontroller.RosterDownloader;
import uk.tomhomewood.android.jmricontroller.Turnout;
import uk.tomhomewood.android.jmricontroller.Utils;
import uk.tomhomewood.android.jmricontroller.RosterDownloader.RosterDownloadListener;

public class WiThrottleCommandClientLists extends WiThrottleCommandClientBase implements RosterDownloadListener {
	private final String TAG = "WiThrottleCommandClientLists";

	public static final String PREFIX_TURNOUT_LIST = "PTL";
	public static final String PREFIX_LOCOMOTIVE_LIST = "RL";
	public static final String PREFIX_WEB_SERVER_PORT = "PW";
	public static final String PREFIX_HEARTBEAT_INTERVAL = "*";

	private static final long WEB_SERVER_TIMEOUT_MS = 500;

	private final String DELIMITER = "]\\[";
	private final String INFO_DELIMITER = "}|{";

	private ListsEventListener listsEventListener;

	private boolean turnoutsListReceived;

	private int webServerPort = -1;
	private String basicLocomotiveListResponse;

	private Handler handler;

	public WiThrottleCommandClientLists(Context context, WiThrottleSocketClient withrottleSocketClient, ListsEventListener listsEventListener) {
		super(context, withrottleSocketClient);
		this.listsEventListener = listsEventListener;
		handler = new Handler();
	}

	private int processTurnoutList(String response) {
		int nTurnoutsAddedToDataBase = 0;
		if(response!=null){
			String[] turnouts = Utils.splitStringByString(response, DELIMITER);
			String[] pieces;
			int nTurnouts = turnouts.length;
			Turnout turnout;
			for(int i=1; i<nTurnouts; i++){
				pieces = Utils.splitStringByString(turnouts[i], INFO_DELIMITER);
				try{
					turnout = new Turnout(pieces[0], pieces[1], Integer.parseInt(pieces[2]));
					database.addTurnout(turnout);
					nTurnoutsAddedToDataBase++;
				}
				catch(NullPointerException e){
					Log.e(TAG, "Error parsing turnout details: "+e.toString());
				}
			}
		}
		listsEventListener.turnoutsAvailable();
		return nTurnoutsAddedToDataBase;
	}

	private int processLocomotiveList(String response) {
		int nLocomotivesAddedToDataBase = 0;

		try{
			String[] responsePieces = Utils.splitStringByString(response, DELIMITER);

			int nLocomotives = responsePieces.length;
			String[] pieces;
			Locomotive locomotive;
			for(int i=1; i<nLocomotives; i++){		//Skip the first address as this is the number of locos in the list
				if(!responsePieces[i].isEmpty()){
					pieces = Utils.splitStringByString(responsePieces[i], INFO_DELIMITER);
					try{
						locomotive = new Locomotive(pieces[0], pieces[2]+pieces[1]);
						database.addLocomotive(locomotive);
						nLocomotivesAddedToDataBase++;
					}
					catch(NullPointerException e){
						Log.e(TAG, "Error parsing locomotive details: "+e.toString());
					}
				}
			}
			if(listsEventListener!=null){
				listsEventListener.locomotivesListAvailable(nLocomotivesAddedToDataBase);
			}
			return nLocomotivesAddedToDataBase;
		}
		catch(Exception e){
			return 0;
		}
	}

	public void emptyDatabase() {
		database.deleteTurnouts();
		database.deleteLocomotives();
	}

	@Override
	public void responseReceived(String response) {
		if(response.startsWith(PREFIX_TURNOUT_LIST)){
			if(!turnoutsListReceived){
				processTurnoutList(response);
				turnoutsListReceived = true;
			}
		}
		else if(response.startsWith(PREFIX_LOCOMOTIVE_LIST)){
			basicLocomotiveListResponse = response;
			startWebServerPortTimeout();
		}
		else if(response.startsWith(PREFIX_WEB_SERVER_PORT)){
			cancelWebServerPortTimeout();
			processWebServerPort(response);

		}
		else if(response.startsWith(PREFIX_HEARTBEAT_INTERVAL)){
			processHeartbeatInterval(response);
		}
		super.responseReceived(response);
	}

	private void cancelWebServerPortTimeout() {
		handler.removeCallbacks(webServerPortTimeoutTask);
	}

	private void startWebServerPortTimeout() {
		handler.postDelayed(webServerPortTimeoutTask, WEB_SERVER_TIMEOUT_MS);
	}

	private void processHeartbeatInterval(String response) {
		if(response!=null && response.length()>1){
			try{
				int heartbeatInterval = Integer.parseInt(response.substring(1));
				listsEventListener.heartbeatIntervalAvailable(heartbeatInterval);
			}
			catch (NumberFormatException e){
				Log.e(TAG, "Error parsing heartbeat interval");
			}
		}
	}

	private void processWebServerPort(String response) {
		try{
			webServerPort = Integer.parseInt(response.substring(2));
			Log.d(TAG, "Got webserver port: "+webServerPort);
			downloadLocomotiveRoster();
		}
		catch(NumberFormatException e){
			Log.e(TAG, "Error parsing web server port: "+e.toString());
		}
	}

	private void downloadLocomotiveRoster(){
		NetworkServiceDescriptor serviceDescriptor = withrottleSocketClient.getNetworkServiceDescriptor();
		if(serviceDescriptor!=null && webServerPort!=-1){
			new RosterDownloader(context, serviceDescriptor.getIpAddress(), webServerPort, this).execute();
		}
	}

	@Override
	public void rosterDownloadComplete(int nLocomotives) {
		if(listsEventListener!=null){
			listsEventListener.locomotivesListAvailable(nLocomotives);
		}
	}

	@Override
	public void rosterDownloadFailed() {
		//Downloading roster from JMRI webserver failed, so fall back to the basic list instead
		if(basicLocomotiveListResponse!=null){
			processLocomotiveList(basicLocomotiveListResponse);
		}
	}

	private Runnable webServerPortTimeoutTask = new Runnable() {
		@Override
		public void run() {
			Log.d(TAG, "Timeout waiting for web server port number, falling back to basic roster processing");
			rosterDownloadFailed();		//This will cause this class to fall back to the basic roster response
		}
	};

	public interface ListsEventListener {

		public void heartbeatIntervalAvailable(int heartbeatInterval);

		public void turnoutsAvailable();

		public void locomotivesListAvailable(int nLocomotives);
	}
}