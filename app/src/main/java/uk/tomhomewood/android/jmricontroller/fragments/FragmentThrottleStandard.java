package uk.tomhomewood.android.jmricontroller.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import uk.tomhomewood.android.jmricontroller.Locomotive;
import uk.tomhomewood.android.jmricontroller.R;

public class FragmentThrottleStandard extends FragmentThrottle {
//	private final String TAG = "FragmentThrottleStandard";

//	private final int REQUEST_LANDSCAPE_THROTTLE = 10;

	private TextView throttleTitle;
	private TextView locomotiveModel;
	
//	private int SCREEN_TYPE_PHONE;

	@Override
	protected int getLayoutResId() {
		return R.layout.fragment_throttle;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View fragmentLayout = super.onCreateView(inflater, container, savedInstanceState);

//		SCREEN_TYPE_PHONE = getResources().getInteger(R.integer.phone);

		throttleTitle = (TextView) fragmentLayout.findViewById(R.id.fragment_throttle_loco_number);
		locomotiveModel = (TextView) fragmentLayout.findViewById(R.id.fragment_throttle_loco_name);
		
		setHasOptionsMenu(true);
		
		return fragmentLayout;
	}
/*	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_throttle, menu);
		//TODO temp
		if(true || getResources().getInteger(R.integer.screen_type)!=SCREEN_TYPE_PHONE){	//Not a phone
			menu.removeItem(R.id.action_full_screen_throttle);
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		switch(item.getItemId()){
		case R.id.action_full_screen_throttle:
			launchLandscapeThrottleActivity();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
*/
/*	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode){
		case REQUEST_LANDSCAPE_THROTTLE:
			Locomotive locomotive = (Locomotive) data.getSerializableExtra(ActivityLandscapeThrottle.EXTRA_RETURNED_LOCOMOTIVE);
			if(locomotive!=null){				//Acquire the locomotive that was being used in the landscape activity
				setLocomotive(locomotive);
			}
			break;
		default:
			super.onActivityResult(requestCode, resultCode, data);
			break;
		}
	}
*/
	@Override
	protected void loadLocomotive(Locomotive locomotive) {
		super.loadLocomotive(locomotive);
		throttleTitle.setText(locomotive.getNumber());
		if(locomotive.getModel()!=null){
			locomotiveModel.setText(locomotive.getModel());
		}
	}

	@Override
	protected void noLocoSelected() {
		super.noLocoSelected();
		throttleTitle.setText("");
	}
/*
	private void launchLandscapeThrottleActivity() {
		Intent intent = new Intent(parentActivity, ActivityLandscapeThrottle.class);
		intent.putExtra(ActivityLandscapeThrottle.EXTRA_THROTTLE_CODE, throttleCommandClient.getThrottleNumber());
		startActivityForResult(intent, REQUEST_LANDSCAPE_THROTTLE);
	}
*/
}