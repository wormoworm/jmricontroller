package uk.tomhomewood.android.jmricontroller.fragments;

import uk.tomhomewood.android.jmricontroller.api.Control;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class JmriControllerFragment extends Fragment implements OnSharedPreferenceChangeListener{
	private final String TAG = "JmriControllerFragment";
	
	private SharedPreferences preferences;
	
	private Control control;

    private Activity activity;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
        Log.d(TAG, "onAttach()");
        control = Control.getInstance(activity);
    }

    protected Activity getParentActivity(){
        return activity;
    }

    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		preferences.registerOnSharedPreferenceChangeListener(this);
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		preferences.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
	}
	
	protected Control getControl(){
		return control;
	}

	protected SharedPreferences getPreferences(){
		return preferences;
	}
}