package uk.tomhomewood.android.jmricontroller;

import java.util.ArrayList;

import uk.tomhomewood.smart.preferences.UniqueStringStorageClient;

import android.content.SharedPreferences;

public class RecentConnectionsClient extends UniqueStringStorageClient {

	private final String KEY_RECENT_ADDRESSES = "recentAddresses";
	private final String KEY_RECENT_PORTS = "recentPorts";

	public RecentConnectionsClient(SharedPreferences preferences) {
		super(preferences);
	}
	
	public ArrayList<String> getAddresses(){
		return super.getValuesByKey(KEY_RECENT_ADDRESSES);
	}
	
	public ArrayList<String> getPorts(){
		return super.getValuesByKey(KEY_RECENT_PORTS);
	}
	
	public void storeAddress(String address){
		super.storeValue(KEY_RECENT_ADDRESSES, address);
	}
	
	public void storePort(int port){
		super.storeValue(KEY_RECENT_PORTS, port+"");
	}
	
	public String getMostRecentAddress(){
		ArrayList<String> addresses = getAddresses();
		if(addresses!=null && !addresses.isEmpty()){
			return addresses.get(addresses.size()-1);
		}
		else{
			return null;
		}
	}
	
	public String getMostRecenPort(){
		ArrayList<String> ports = getPorts();
		if(ports!=null && !ports.isEmpty()){
			return ports.get(ports.size()-1);
		}
		else{
			return null;
		}
	}
}