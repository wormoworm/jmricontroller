package uk.tomhomewood.android.jmricontroller.api;

import uk.tomhomewood.android.jmricontroller.network.WiThrottleCommandClientCore.PowerState;


public interface LayoutControlListener {

	public void powerStateChanged(PowerState powerState);
	
	public void turnoutsListAvailable();
	
	public void locomotiveListAvailable();
}