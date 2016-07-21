package uk.tomhomewood.android.jmricontroller.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

import android.os.SystemClock;
import android.util.Log;

public class WiThrottleSocket extends Thread {
	private final String TAG = "WiThrottleSocket";
	
	protected NetworkServiceDescriptor networkServiceDescriptor;
	private WithrottleSocketResponseListener withrottleSocketResponseListener;
	
	protected Socket clientSocket = null;
	protected BufferedReader inputBR = null;
	protected PrintWriter outputPW = null;
	private volatile boolean endRead = false;		//Signals rcvr to terminate
	public volatile boolean socketGood = false;		//Indicates socket condition

	public WiThrottleSocket(NetworkServiceDescriptor networkServiceDescriptor, WithrottleSocketResponseListener withrottleSocketResponseListener) {
		super("socket_WiT");
		this.networkServiceDescriptor = networkServiceDescriptor;
		this.withrottleSocketResponseListener = withrottleSocketResponseListener;
	}

	public boolean connect() {
		Log.d(TAG, "CONNECTED");

		//use local socketOk instead of setting socketGood so that the rcvr doesn't resume until connect() is done
		boolean socketOk = HaveNetworkConnection();	
/*
		//validate address
		if (socketOk) {
			try { 
				//host_address=InetAddress.getByName(hostAddress); 
			}
			catch(UnknownHostException except) {
				process_comm_error("Can't determine IP address of " + hostAddress);
				socketOk = false;
			}
		}
*/
		//socket
		if (socketOk) {
			try {
				clientSocket = new Socket();               //look for someone to answer on specified socket, and set timeout
				InetSocketAddress socketAddress = new InetSocketAddress(networkServiceDescriptor.getIpAddress(), networkServiceDescriptor.getPortNumber());
				clientSocket.connect(socketAddress, 3000);  //TODO: adjust these timeouts, or set in prefs
				clientSocket.setSoTimeout(500);
			}
			catch(Exception e)  {
				Log.e(TAG, "Can't connect to host: "+e.toString());
				socketOk = false;
			}
		}

		//rcvr
		if (socketOk) {
			try {
				inputBR = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			}
			catch (IOException except) {
				process_comm_error("Error creating input stream, IOException: "+except.getMessage());
				socketOk = false;
			} 
		}

		//start the socket_WiT thread.
		if (socketOk) {
			if (!this.isAlive()) {
				endRead = false;
				try {
					this.start();
				} catch (IllegalThreadStateException except) {
					//ignore "already started" errors
					process_comm_error("Error starting socket_WiT thread:  "+except.getMessage());
				}
			}
		}

		//xmtr
		if (socketOk) {
			try { 
				outputPW = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true); 
			}
			catch(IOException e) {
				process_comm_error("Error creating output stream, IOException: "+e.getMessage());
				socketOk = false;
			}
		}
		socketGood = socketOk;
		return socketOk;
	}

	public void disconnect(boolean shutdown) {
		if (shutdown) {
			endRead = true;
			for(int i = 0; i < 5 && this.isAlive(); i++) {
				try { 
					Thread.sleep(300);    			//  give run() a chance to see endRead and exit
				}
				catch (InterruptedException e) { 
					process_comm_error("Error sleeping the thread, InterruptedException: "+e.getMessage());
				}
			}
		}

		socketGood = false;

		//close socket
		if (clientSocket != null) {
			try { 
				clientSocket.close();
			}
			catch(Exception e) { 
				Log.d(TAG,"Error closing the Socket: "+e.toString()); 
			}
		}

		if (!shutdown)	// going to retry the connection
		{
			// reinit shared variables then signal activities to refresh their views
			// so that (potentially) invalid information is not displayed
//			initShared();
//			sendMsg(comm_msg_handler, message_type.WIT_CON_RETRY);
		}
	}
	
	public boolean isConnected(){
		return socketGood;
	}
	
	public NetworkServiceDescriptor getNetworkServiceDescriptor(){
		return networkServiceDescriptor;
	}

	//read the input buffer
	public void run() {
		String response = null;
		//continue reading until signalled to exit by endRead
		while(!endRead) {
			if(socketGood) {		//skip read when the socket is down
				///*
				try {
					if((response = inputBR.readLine()) != null) {
						if (response.length()>0) {
//							heart.restartInboundInterval();
							if(withrottleSocketResponseListener!=null){
								withrottleSocketResponseListener.responseReceived(response);
							}
						}
					}
				} 
				catch (SocketTimeoutException e )   {
					socketGood = this.SocketCheck();
				} 
				catch (IOException e) {
					if(socketGood) {
						Log.d(TAG,"WiT rcvr error.");
						socketGood = false;		//input buffer error so force reconnection on next send
					}
				}
				//*/
			}
			if(!socketGood) {
				SystemClock.sleep(1000L);	//don't become compute bound here when the socket is down
			}
		}
//		heart.stopHeartbeat();
	}

	public boolean send(String message) {
		//reconnect socket if needed
		if(!socketGood || !this.SocketCheck()) {
			this.disconnect(false);		//clean up socket but do not shut down the receiver
			this.connect();				//attempt to reestablish connection
			if(socketGood) {
				process_comm_error("Success: Restored connection to WiThrottle server " + networkServiceDescriptor.getIpAddress() + ".\n");
//				sendMsg(comm_msg_handler, MessageTypes.WIT_CON_RECONNECT);
			}
			else {
				return false;
			}
		}

		//Send the message
		if(socketGood) {
			try {
				outputPW.println(message);
				outputPW.flush();
//				Log.d(TAG, "Sent: "+message);
			} 
			catch (Exception e) {
				Log.d("Engine_Driver","WiT xmtr error.");
				socketGood = false;		//output buffer error so force reconnection on next send
				return false;
			}
		}
		return true;		//If we reach here, everything went OK
	}

	// attempt to determine if the socket connection is still good
	public boolean SocketCheck() {
		boolean status = clientSocket.isConnected() && !clientSocket.isInputShutdown() && !clientSocket.isOutputShutdown();
		if (status)
			status = HaveNetworkConnection();	// can't trust the socket flags so try something else...
		return status;
	}

	// temporary - SocketCheck should determine whether socket connection is good however socket flags sometimes do not get updated
	// so it doesn't work.  This is better than nothing though?
	private boolean HaveNetworkConnection() {
/*		
		boolean haveConnectedWifi = false;
		boolean haveConnectedMobile = false;

		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo[] netInfo = cm.getAllNetworkInfo();
		for (NetworkInfo ni : netInfo)
		{
			if ("WIFI".equalsIgnoreCase(ni.getTypeName()))
				if (ni.isConnected())
					haveConnectedWifi = true;
			if ("MOBILE".equalsIgnoreCase(ni.getTypeName()))
				if (ni.isConnected())
					haveConnectedMobile = true;
		}
		return haveConnectedWifi || haveConnectedMobile;
*/
		return true;
	}
	
	private void process_comm_error(final String msg_txt) {
		
	}
	
	public interface WithrottleSocketResponseListener{
		
		public void responseReceived(String response);
	}
}