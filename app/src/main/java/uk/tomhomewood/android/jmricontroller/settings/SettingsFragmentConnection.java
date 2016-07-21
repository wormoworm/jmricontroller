package uk.tomhomewood.android.jmricontroller.settings;

import uk.tomhomewood.android.jmricontroller.R;
import android.os.Bundle;

public class SettingsFragmentConnection extends SettingsFragment {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings_connection);
	}
}