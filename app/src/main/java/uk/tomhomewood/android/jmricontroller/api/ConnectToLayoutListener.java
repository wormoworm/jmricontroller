package uk.tomhomewood.android.jmricontroller.api;

import uk.tomhomewood.android.jmricontroller.network.NetworkServiceDescriptor;

public interface ConnectToLayoutListener {

	public void connectedToLayout(NetworkServiceDescriptor serviceDescriptor);
	
	public void connectToLayoutFailed();
}