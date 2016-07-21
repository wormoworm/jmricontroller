package uk.tomhomewood.android.jmricontroller.fragments;

import android.util.Log;
import uk.tomhomewood.android.jmricontroller.Locomotive;
import uk.tomhomewood.android.jmricontroller.R;

public class FragmentThrottleLandscapePhone extends FragmentThrottle {
	private final String TAG = "FragmentThrottleLandscapePhone";

	@Override
	protected int getLayoutResId() {
		return R.layout.fragment_throttle_landscape_phone;
	}
	
	@Override
	protected void loadLocomotive(Locomotive locomotive) {
		Log.d(TAG, "loadLocomotive()");
		super.loadLocomotive(locomotive);
        getParentActivity().getActionBar().setTitle(locomotive.getNumber());
		if(locomotive.getModel()!=null){
            getParentActivity().getActionBar().setSubtitle(locomotive.getModel());
		}
	}
}