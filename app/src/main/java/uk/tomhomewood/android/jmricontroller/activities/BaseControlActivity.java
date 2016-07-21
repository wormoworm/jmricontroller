package uk.tomhomewood.android.jmricontroller.activities;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import uk.tomhomewood.android.jmricontroller.R;
import uk.tomhomewood.android.jmricontroller.api.Control;
import uk.tomhomewood.android.jmricontroller.api.LayoutControlListener;
import uk.tomhomewood.android.jmricontroller.network.WiThrottleCommandClientCore.PowerState;
import uk.tomhomewood.android.jmricontroller.settings.Settings;
import uk.tomhomewood.smart.activities.SmartFragmentActivity;

public abstract class BaseControlActivity extends BaseActivity implements LayoutControlListener, OnSharedPreferenceChangeListener{
//	private final String TAG = "BaseControlActivity";
	
	private SharedPreferences preferences;
	
	protected Control control;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		control = Control.getInstance(this);
		control.addLayoutControlListener(this);

		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		
		preferences.registerOnSharedPreferenceChangeListener(this);
		
		checkKeepScreenOn();
		
		forceActionBarOverflow();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		preferences.unregisterOnSharedPreferenceChangeListener(this);
		control.removeLayoutControlListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if(key.equals(Settings.PREFS_KEY_KEEP_SCREEN_ON)){
			checkKeepScreenOn();
		}
		else if(key.equals(Settings.PREFS_KEY_SHOW_EMERGENCY_STOP_BUTTON)){
			invalidateOptionsMenu();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_base_control, menu);
		if(!preferences.getBoolean(Settings.PREFS_KEY_SHOW_EMERGENCY_STOP_BUTTON, false)){
			menu.removeItem(R.id.action_emergency_stop);
		}
		if(getPowerState()==PowerState.ON){
			menu.removeItem(R.id.action_layout_power_on);
		}
		else {
			menu.removeItem(R.id.action_layout_power_off);
		}
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		switch(item.getItemId()){
		case android.R.id.home:
			finish();
			return true;
		case R.id.action_settings:
			ActivitySettings.launch(this);
			return true;
		case R.id.action_emergency_stop:
			control.emergencyStopAllThrottles();
			return true;
		case R.id.action_layout_power_on:
			control.getCoreCommandClient().turnLayoutPowerOn(null);
			return true;
		case R.id.action_layout_power_off:
			control.getCoreCommandClient().turnLayoutPowerOff(null);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void powerStateChanged(PowerState powerState) {
		invalidateOptionsMenu();
	}

	@Override
	public void turnoutsListAvailable() {
	}

	@Override
	public void locomotiveListAvailable() {
	}

	protected PowerState getPowerState(){
		return control.getCoreCommandClient().getLayoutPowerState();
	}
	
	protected SharedPreferences getPreferences(){
		return preferences;
	}
	
	private void checkKeepScreenOn() {
		if(getPreferences().getBoolean(Settings.PREFS_KEY_KEEP_SCREEN_ON, false)){
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
		else{
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
	}

}