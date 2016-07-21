package uk.tomhomewood.android.jmricontroller.api;

import java.io.IOException;
import java.net.Inet4Address;

import javax.jmdns.JmDNS;

import uk.tomhomewood.android.jmricontroller.network.WiThrottleServiceListener;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Handler;
import android.util.Log;

public class JMDNSClient {
	private final String TAG = "JMDNSClient";

	private final String SERVICE_LISTENER_TYPE = "_withrottle._tcp.local.";

	private Context context;
	
	private JmDNS jmdns;
	private volatile boolean endingJmdns;
	private MulticastLock multicastLock;
	private WiThrottleServiceListener withrottleListener;
	
	private Handler handler;
	private JmdnsStatusListener jmdnsStatusListener;
	
	public JMDNSClient(Context context, WiThrottleServiceListener wiThrottleServiceListener, JmdnsStatusListener jmdnsStatusListener) {
		this.context = context;
		this.withrottleListener = wiThrottleServiceListener;
		this.jmdnsStatusListener = jmdnsStatusListener;
		handler = new Handler();
	}
	
	public void startJmdns() {
		int intaddr = 0;
		//Set up to find a WiThrottle service via ZeroConf
		try {
			WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			WifiInfo wifiinfo = wifi.getConnectionInfo();
			intaddr = wifiinfo.getIpAddress();
			if (intaddr != 0) {

				if (multicastLock == null) { // do this only as needed
					multicastLock = wifi.createMulticastLock("jmri_controller");
					multicastLock.setReferenceCounted(true);
				}

				byte[] byteaddr = new byte[] { (byte) (intaddr & 0xff),
						(byte) (intaddr >> 8 & 0xff),
						(byte) (intaddr >> 16 & 0xff),
						(byte) (intaddr >> 24 & 0xff) };
				Inet4Address addr = (Inet4Address) Inet4Address
						.getByAddress(byteaddr);
				String clientAddress = addr.toString().substring(1);

				jmdns = JmDNS.create(addr, clientAddress); //Pass ip as name to avoid hostname lookup attempt
				if(jmdnsStatusListener!=null){
					handler.post(new Runnable() {
						@Override
						public void run() {
							jmdnsStatusListener.jmdnsStarted();
						}
					});
				}
			}
		}
		catch (IOException e) {
			Log.e("Engine_Driver", "start_jmdns - Error creating withrottle listener: "+ e.getMessage());
		}
	}

	//endJmdns() takes a long time, so put it in its own thread
	public void endJmdns() {
		if (jmdnsIsActive()) { // only need to run one instance of this thread
			// to terminate jmdns
			endingJmdns = true;
			Thread jmdnsThread = new Thread() {
				@Override
				public void run() {
					if(jmdnsStatusListener!=null){
						handler.post(new Runnable() {
							@Override
							public void run() {
								jmdnsStatusListener.jmdnsCloseBegun();
							}
						});
					}
					try {
						jmdns.removeServiceListener(SERVICE_LISTENER_TYPE, withrottleListener);
						multicastLock.release();
					}
					catch (Exception e) {
						Log.d(TAG, "Exception whilst removing Jmdns service listener: "+e.toString());
					}
					try {
						jmdns.close();
					}
					catch (Exception e) {
						Log.d(TAG, "Exception whilst closing Jmdns: "+e.toString());
					}
					jmdns = null;
					endingJmdns = false;
					if(jmdnsStatusListener!=null){
						handler.post(new Runnable() {
							@Override
							public void run() {
								jmdnsStatusListener.jmdnsCloseComplete();
							}
						});
					}
				}
			};
			jmdnsThread.start();
		}
	}

	public boolean jmdnsIsActive() {
		return jmdns != null && !endingJmdns;
	}
	
	public boolean jmdnsIsEnding(){
		return endingJmdns;
	}

	public void addServiceListener(WiThrottleServiceListener withrottleListener) {
		try {
			multicastLock.acquire();
		}
		catch (Exception e) {
			Log.d(TAG, "MulticastLock.acquire() failed");
		}
		jmdns.addServiceListener(SERVICE_LISTENER_TYPE, withrottleListener);
	}
	
	public interface JmdnsStatusListener{
		
		public void jmdnsStarted();
		
		public void jmdnsCloseBegun();
		
		public void jmdnsCloseComplete();
	}
}