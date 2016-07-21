package uk.tomhomewood.android.jmricontroller.api;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import uk.tomhomewood.android.jmricontroller.Heartbeat;
import uk.tomhomewood.android.jmricontroller.api.JMDNSClient.JmdnsStatusListener;
import uk.tomhomewood.android.jmricontroller.network.NetworkServiceDescriptor;
import uk.tomhomewood.android.jmricontroller.network.WiThrottleCommandClientCore;
import uk.tomhomewood.android.jmricontroller.network.WiThrottleCommandClientCore.CoreEventListener;
import uk.tomhomewood.android.jmricontroller.network.WiThrottleCommandClientCore.PowerState;
import uk.tomhomewood.android.jmricontroller.network.WiThrottleCommandClientLists;
import uk.tomhomewood.android.jmricontroller.network.WiThrottleCommandClientLists.ListsEventListener;
import uk.tomhomewood.android.jmricontroller.network.WiThrottleCommandClientThrottle;
import uk.tomhomewood.android.jmricontroller.network.WiThrottleCommandClientTurnouts;
import uk.tomhomewood.android.jmricontroller.network.WiThrottleServiceListener;
import uk.tomhomewood.android.jmricontroller.network.WiThrottleSocket;
import uk.tomhomewood.android.jmricontroller.network.WiThrottleSocketClient;
import uk.tomhomewood.android.jmricontroller.network.WiThrottleServiceListener.WithrottleEventListener;
import uk.tomhomewood.android.jmricontroller.network.WiThrottleSocketClient.WithrottleSocketListener;

import android.content.Context;
import android.util.Log;
import android.util.SparseArray;

public class Control implements WithrottleEventListener, WithrottleSocketListener, JmdnsStatusListener {
	private final String TAG = "Control";

	private static final int N_THROTTLE_COMMAND_CLIENTS = 3;

	private Context context;
	
	//These objects are used for scanning for layouts
	private JMDNSClient jmdnsClient;
	private WiThrottleServiceListener withrottleListener;
	private List<LayoutScanListener> layoutScanListeners;

	//This is used for managing the underlying socket
	private WiThrottleSocketClient withrottleSocketClient;
	
	//These object are used for interactions with the server
	private String throttleName;
	private boolean isConnectedToLayout;
	private ConnectToLayoutListener connectToLayoutListener;
	private WiThrottleCommandClientCore coreCommandClient;
	private WiThrottleCommandClientLists listsCommandClient;
	private List<LayoutControlListener> layoutControlListeners;
	private Heartbeat heartbeat;
	private WiThrottleCommandClientTurnouts turnoutsCommandClient;
	private SparseArray<WiThrottleCommandClientThrottle> throttleCommandClients;
	
	private static Control instance;

	private Runnable jmdnsCloseRunnable;
    private NetworkServiceDescriptor connectionNetworkServiceDescriptor;

    public static Control getInstance(Context context){
		if(instance==null){
			instance = new Control(context);
		}
		return instance;
	}
	
	private Control(Context context){
		this.context = context;
		
		withrottleListener = new WiThrottleServiceListener(this);
		jmdnsClient = new JMDNSClient(context, withrottleListener, this);
		
		layoutScanListeners = new ArrayList<LayoutScanListener>();
		layoutControlListeners = new ArrayList<LayoutControlListener>();
	}
	
	public Context getContext(){
		return context;
	}
	
	public void addLayoutScanListener(LayoutScanListener layoutScanListener){
		layoutScanListeners.add(layoutScanListener);
	}
	
	public void removeLayoutScanListener(LayoutScanListener layoutScanListener){
		layoutScanListeners.remove(layoutScanListener);
	}
	
	public void startLayoutScan(){
		if(!jmdnsClient.jmdnsIsActive()){		//Only start JMDNS if it is not already running
			if(jmdnsClient.jmdnsIsEnding()){	//If JMDNS is currently being closed down, post a delayed Runnable task to run this method once it has been closed down
				jmdnsCloseRunnable = new Runnable() {
					@Override
					public void run() {
						startLayoutScan();
					}
				};
			}
			else{
				jmdnsClient.startJmdns();
				if(jmdnsClient.jmdnsIsActive()) { // TODO merge this into jmdnsClient.startJmdns()?
					jmdnsClient.addServiceListener(withrottleListener);
				}
				else{
					Log.d(TAG, "JMDNS could not be started");
				}
			}
		}
	}
	
	public void stopLayoutScan(){
		jmdnsClient.endJmdns();
	}
	
	public void connectToLayout(NetworkServiceDescriptor serviceDescriptor, String throttleName, ConnectToLayoutListener connectToLayoutListener){
		this.connectToLayoutListener = connectToLayoutListener;
		this.throttleName = throttleName;
		withrottleSocketClient = new WiThrottleSocketClient(serviceDescriptor, this);
		withrottleSocketClient.connectSocket();
	}
	
	public void disconnectFromLayout(){
		if(isConnectedToLayout()){
			if(coreCommandClient!=null){
				coreCommandClient.disconnectFromServer(true, null);
			}
			if(listsCommandClient!=null){
				listsCommandClient.emptyDatabase();
			}
			if(heartbeat!=null && heartbeat.isRunning()){
				heartbeat.stop();
			}
			isConnectedToLayout = false;
		}
	}

	private boolean isConnectedToLayout() {
		return isConnectedToLayout;
	}

    public NetworkServiceDescriptor getConnectionNetworkServiceDescriptor(){
        return connectionNetworkServiceDescriptor;
    }

	@Override
	public void serviceResolved(NetworkServiceDescriptor serviceDescriptor) {
		Iterator<LayoutScanListener> iterator = layoutScanListeners.iterator();
		while(iterator.hasNext()){
			iterator.next().layoutFound(serviceDescriptor);
		}
	}

	@Override
	public void serviceRemoved(NetworkServiceDescriptor serviceDescriptor) {
		Iterator<LayoutScanListener> iterator = layoutScanListeners.iterator();
		while(iterator.hasNext()){
			iterator.next().layoutRemoved(serviceDescriptor);
		}
	}

	@Override
	public void connectionSucceeded(WiThrottleSocket withrottleSocket) {
		isConnectedToLayout = true;
		initialiseCommandClients();
		coreCommandClient.sendThrottleName(throttleName, true, null);
		if(connectToLayoutListener!=null){
			connectToLayoutListener.connectedToLayout(withrottleSocket.getNetworkServiceDescriptor());
		}
        connectionNetworkServiceDescriptor = withrottleSocket.getNetworkServiceDescriptor();
		heartbeat = new Heartbeat(context, withrottleSocketClient);
	}

	@Override
	public void connectionFailed() {
		if(connectToLayoutListener!=null){
			connectToLayoutListener.connectToLayoutFailed();
		}
	}

	@Override
	public void disconnectionSucceeded() {
		// TODO Do we need to do anything here?
	}

	@Override
	public void disconnectionFailed() {
		// TODO Do we need to do anything here?
	}

	private void initialiseCommandClients() {
		coreCommandClient = new WiThrottleCommandClientCore(context, withrottleSocketClient, coreEventListener);
		
		listsCommandClient = new WiThrottleCommandClientLists(context, withrottleSocketClient, listsEventListener);
		
		turnoutsCommandClient = new WiThrottleCommandClientTurnouts(context, withrottleSocketClient);
		
		throttleCommandClients = new SparseArray<WiThrottleCommandClientThrottle>(N_THROTTLE_COMMAND_CLIENTS);
		for(int i=0; i<N_THROTTLE_COMMAND_CLIENTS; i++){
			throttleCommandClients.put(i, new WiThrottleCommandClientThrottle(context, withrottleSocketClient, i));
		}
	}
	
	public void emergencyStopAllThrottles(){
		int nThrottleCommandClients = throttleCommandClients.size();
		WiThrottleCommandClientThrottle throttleCommandClient;
		for(int i=0; i<nThrottleCommandClients; i++){
			throttleCommandClient = throttleCommandClients.get(throttleCommandClients.keyAt(i));
			if(throttleCommandClient.hasAcquiredLocomotive()){
				throttleCommandClient.emergencyStop();
			}
		}
	}
	
	public WiThrottleCommandClientCore getCoreCommandClient(){
		return coreCommandClient;
	}
	
	public WiThrottleCommandClientTurnouts getTurnoutsCommandClient(){
		return turnoutsCommandClient;
	}
	
	public WiThrottleCommandClientThrottle getThrottleCommandClient(int throttleNumber){
		if(throttleNumber<N_THROTTLE_COMMAND_CLIENTS){
			return throttleCommandClients.get(throttleNumber);
		}
		else{
			return null;
		}
	}
	
	public void addLayoutControlListener(LayoutControlListener layoutControlListener){
		layoutControlListeners.add(layoutControlListener);
	}
	
	public void removeLayoutControlListener(LayoutControlListener layoutControlListener){
		layoutControlListeners.remove(layoutControlListener);
	}

	public String getThrottleName() {
		return throttleName;
	}
	
	private CoreEventListener coreEventListener = new CoreEventListener() {
		
		@Override
		public void layoutPowerStateChanged(PowerState newPowerState) {
			Iterator<LayoutControlListener> iterator = layoutControlListeners.iterator();
			while(iterator.hasNext()){
				iterator.next().powerStateChanged(newPowerState);
			}
		}
	};

	private ListsEventListener listsEventListener = new ListsEventListener() {

		@Override
		public void turnoutsAvailable() {
//			Log.d(TAG, "turnoutsAvailable()");
			turnoutsCommandClient.setReady();
			Iterator<LayoutControlListener> iterator = layoutControlListeners.iterator();
			while(iterator.hasNext()){
				iterator.next().turnoutsListAvailable();
			}
		}
		
		@Override
		public void locomotivesListAvailable(int nLocomotives) {
			Log.d(TAG, "locomotivesListAvailable()");
			int nThrottleCommandClients = throttleCommandClients.size();
			for(int i=0; i<nThrottleCommandClients; i++){
                Log.d(TAG, "Set ready");
				throttleCommandClients.get(throttleCommandClients.keyAt(i)).setReady();
			}
			Iterator<LayoutControlListener> iterator = layoutControlListeners.iterator();
			while(iterator.hasNext()){
				iterator.next().locomotiveListAvailable();
			}
		}
		
		@Override
		public void heartbeatIntervalAvailable(int heartbeatInterval) {
			if(heartbeat!=null && !heartbeat.isRunning()){
				heartbeat.start(heartbeatInterval * 500);
			}
		}
	};

	@Override
	public void jmdnsStarted() {
	}

	@Override
	public void jmdnsCloseBegun() {
	}

	@Override
	public void jmdnsCloseComplete() {
		if(jmdnsCloseRunnable!=null){
			jmdnsCloseRunnable.run();
		}
	}
}