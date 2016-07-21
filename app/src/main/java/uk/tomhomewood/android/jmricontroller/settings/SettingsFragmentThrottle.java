package uk.tomhomewood.android.jmricontroller.settings;

import uk.tomhomewood.android.jmricontroller.R;
import android.os.Bundle;

public class SettingsFragmentThrottle extends SettingsFragment {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings_throttle);
	}
}