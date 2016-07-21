package uk.tomhomewood.android.jmricontroller.network;

import java.util.ArrayList;
import java.util.Iterator;

import uk.tomhomewood.android.jmricontroller.network.WiThrottleCommandClientBase.SocketCommandListener;
import uk.tomhomewood.android.jmricontroller.network.WiThrottleSocket.WithrottleSocketResponseListener;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class WiThrottleSocketClient implements WithrottleSocketResponseListener {
	private final String TAG = "WiThrottleSocketClient";

	private NetworkServiceDescriptor networkServiceDescriptor;
	private WithrottleSocketListener withrottleSocketListener;
	private WiThrottleSocket withrottleSocket;
	private WiThrottleSocketClient socketClient;

	private Handler handler;

	private ArrayList<WithrottleResponseListener> withrottleResponseListeners;

	public WiThrottleSocketClient(NetworkServiceDescriptor networkServiceDescriptor, WithrottleSocketListener withrottleSocketListener){
		this.networkServiceDescriptor = networkServiceDescriptor;
		this.withrottleSocketListener = withrottleSocketListener;
		socketClient = this;
		handler = new Handler();
		withrottleResponseListeners = new ArrayList<WithrottleResponseListener>();
	}

	public NetworkServiceDescriptor getNetworkServiceDescriptor(){
		return networkServiceDescriptor;
	}

	public void connectSocket() {
		Thread connectThread = new Thread() {
			@Override
			public void run() {
				//avoid duplicate connects, seen when user clicks address multiple times quickly
				if (withrottleSocket != null && withrottleSocket.socketGood) {
					Log.d(TAG ,"Duplicate CONNECT message received.");
					return;
				}

				//attempt connection to WiThrottle server
				withrottleSocket = new WiThrottleSocket(networkServiceDescriptor, socketClient);
				if(withrottleSocketListener!=null){
					if(withrottleSocket.connect()) {
						handler.post(new Runnable() {
							@Override
							public void run() {
								withrottleSocketListener.connectionSucceeded(withrottleSocket);
							}
						});
					}
					else{
						handler.post(new Runnable() {
							@Override
							public void run() {
								withrottleSocketListener.connectionFailed();
							}
						});
					}
				}
			}
		};
		connectThread.start();
	}

	public void disconnectSocket(){
		Thread disconnectThread = new Thread(){
			@Override
			public void run() {
				if(withrottleSocket!=null){
					withrottleSocket.disconnect(true);
				}
				if(withrottleSocketListener!=null){
					handler.post(new Runnable() {
						@Override
						public void run() {
							withrottleSocketListener.disconnectionSucceeded();
						}
					});
				}
			}
		};
		disconnectThread.start();
	}

	public void sendCommand(final SocketCommandListener commandListener, final int requestCode, final String command, final Bundle extras){ 	
		Thread sendDataThread = new Thread(){
			@Override
			public void run(){
				if(withrottleSocket != null && commandListener!=null){
					if(withrottleSocket.send(command)){
						commandListener.commandSent(requestCode, extras);
					}
					else{
						commandListener.commandFailed(requestCode, extras);
					}
				}
			}
		};
		sendDataThread.start();
	}

	public boolean addWithrottleResponseListener(WithrottleResponseListener withrottleResponseListener){
		if(withrottleResponseListener!=null){
			withrottleResponseListeners.add(withrottleResponseListener);
			return true;
		}
		else{
			return false;
		}
	}

	public boolean removeWithrottleResponseListener(WithrottleResponseListener withrottleResponseListener){
		if(withrottleResponseListener!=null){
			withrottleResponseListeners.remove(withrottleResponseListener);
			return true;
		}
		else{
			return false;
		}
	}
/*
	private String[] splitByString(String input, String divider) {
		int size = 0;
		String temp = input;

		// count entries
		while(temp.length() > 0) {
			size++;
			int index = temp.indexOf(divider);
			if (index < 0) break;    // break not found
			temp = temp.substring(index+divider.length());
			if (temp.length() == 0) {  // found at end
				size++;
				break;
			}
		}

		String[] result = new String[size];

		// find entries
		temp = input;
		size = 0;
		while(temp.length() > 0) {
			int index = temp.indexOf(divider);
			if (index < 0) break;    // done with all but last
			result[size] = temp.substring(0,index);
			temp = temp.substring(index+divider.length());
			size++;
		}
		result[size] = temp;

		return result;
	}
*/
	@Override
	public void responseReceived(final String response) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				sendResponseToResponseListeners(response);
			}
		});
	}

	private void sendResponseToResponseListeners(String response) {
		Iterator<WithrottleResponseListener> iterator = withrottleResponseListeners.iterator();
		while(iterator.hasNext()){
			iterator.next().responseReceived(response);
		}
	}

	public interface WithrottleSocketListener{
		public void connectionSucceeded(WiThrottleSocket withrottleSocket);

		public void connectionFailed();

		public void disconnectionSucceeded();

		public void disconnectionFailed();
	}

	public interface WithrottleResponseListener{

		public void responseReceived(String response);
	}

	public boolean isSocketConnected() {
		return withrottleSocket.isConnected();
	}
}