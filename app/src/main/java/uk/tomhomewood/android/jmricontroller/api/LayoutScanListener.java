package uk.tomhomewood.android.jmricontroller.api;

import uk.tomhomewood.android.jmricontroller.network.NetworkServiceDescriptor;

public interface LayoutScanListener {

	public void layoutFound(NetworkServiceDescriptor serviceDescriptor);
	
	public void layoutRemoved(NetworkServiceDescriptor serviceDescriptor);
}