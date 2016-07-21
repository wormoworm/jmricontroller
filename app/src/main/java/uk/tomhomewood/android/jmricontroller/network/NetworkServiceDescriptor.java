package uk.tomhomewood.android.jmricontroller.network;

import java.io.Serializable;
import java.net.Inet4Address;

public class NetworkServiceDescriptor implements Serializable {
	//private final static String TAG = "NetworkServiceDescriptor";

	private static final long serialVersionUID = 1L;

	protected Inet4Address ipAddress;
	protected int portNumber;
	protected String hostName;

	public NetworkServiceDescriptor(Inet4Address ipAddress, int portNumber, String hostName) {
		this.ipAddress = ipAddress;
		this.portNumber = portNumber;
		this.hostName = hostName;
	}

	public Inet4Address getIpAddress() {
		return ipAddress;
	}

	public int getPortNumber() {
		return portNumber;
	}

	public String getHostName() {
		return hostName;
	}
	
	public boolean equals(NetworkServiceDescriptor networkServiceDescriptor){
		return networkServiceDescriptor.hostName.equals(hostName);
	}

	public String getAddressString() {
		String address = null;
		if(ipAddress!=null){
			address = ipAddress.toString().substring(1);
		}
		return address;
	}
}