package uk.tomhomewood.android.jmricontroller.network;

import java.net.Inet4Address;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import android.os.Handler;

public class WiThrottleServiceListener implements ServiceListener{
	//private final String TAG = "WiThrottleServiceListener";
	
	private WithrottleEventListener withrottleEventListener;
	private Handler handler;

	public WiThrottleServiceListener(WithrottleEventListener withrottleEventListener){
		this.withrottleEventListener = withrottleEventListener;
		handler = new Handler();
	}
	
	private NetworkServiceDescriptor createNetworkServiceDescriptorFromServiceEvent(ServiceEvent serviceEvent){
		ServiceInfo serviceInfo = serviceEvent.getInfo();
		Inet4Address[] ipAddresses = serviceInfo.getInet4Addresses();  //only get ipV4 address
		int portNumber = serviceInfo.getPort();
		String hostName = serviceInfo.getName();
		Inet4Address ipAddress = ipAddresses.length>0? ipAddresses[0] : null;
		
		return new NetworkServiceDescriptor(ipAddress, portNumber, hostName);
	}

	public void serviceAdded(ServiceEvent event) {
		//    		Log.d("Engine_Driver", String.format("serviceAdded fired"));
		//A service has been added. If no details, ask for them 
		//ServiceInfo si = jmdns.getServiceInfo(event.getType(), event.getName(), 0);
//		if (si == null || si.getPort() == 0 ) { 
//			Log.d("Engine_Driver", String.format("serviceAdded, requesting details: '%s', Type='%s'", event.getName(), event.getType()));
//			jmdns.requestServiceInfo(event.getType(), event.getName(), true, (long)1000);
//		}
	};

	public void serviceRemoved(final ServiceEvent serviceEvent) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				sendServiceRemovedEvent(serviceEvent);
			}
		});
//		sendMsg(connection_msg_handler, MessageTypes.SERVICE_REMOVED, event.getName());	//send the service name to be removed
//		Log.d("Engine_Driver", String.format("serviceRemoved: '%s'", event.getName()));
	};

	public void serviceResolved(final ServiceEvent serviceEvent) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				sendServiceResolvedEvent(serviceEvent);
			}
		});
	};

	private void sendServiceResolvedEvent(ServiceEvent serviceEvent) {
		NetworkServiceDescriptor descriptor = createNetworkServiceDescriptorFromServiceEvent(serviceEvent);
		if(withrottleEventListener!=null){
			withrottleEventListener.serviceResolved(descriptor);
		}
	}
	
	private void sendServiceRemovedEvent(ServiceEvent serviceEvent) {
		NetworkServiceDescriptor descriptor = createNetworkServiceDescriptorFromServiceEvent(serviceEvent);
		if(withrottleEventListener!=null){
			withrottleEventListener.serviceRemoved(descriptor);
		}
	}

	public interface WithrottleEventListener{
		public void serviceRemoved(NetworkServiceDescriptor serviceDescriptor);
		
		public void serviceResolved(NetworkServiceDescriptor serviceDescriptor);
	}
}
