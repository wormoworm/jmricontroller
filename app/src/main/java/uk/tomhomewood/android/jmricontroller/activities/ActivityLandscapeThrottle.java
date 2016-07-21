package uk.tomhomewood.android.jmricontroller.activities;

import uk.tomhomewood.android.jmricontroller.R;
import uk.tomhomewood.android.jmricontroller.fragments.FragmentThrottle;
import uk.tomhomewood.android.jmricontroller.fragments.FragmentThrottleLandscapePhone;
import uk.tomhomewood.android.jmricontroller.fragments.FragmentTurnoutsList;
import android.annotation.TargetApi;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class ActivityLandscapeThrottle extends BaseControlActivity {
//	private final String TAG = "ActivityLandscapeThrottle";

	public static final String EXTRA_THROTTLE_CODE = "throttleCode";

	private FragmentThrottle throttleFragment;
	private FragmentTurnoutsList turnoutsListFragment;
	
	private RelativeLayout turnoutsDrawer;

	private boolean turnoutsDrawerVisible;
	
	private Animation slideOutToLeft, slideInFromLeft;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		forceManualOrientation();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_landscape_throttle);
		
		turnoutsDrawerVisible = false;

		int throttleNumber = getIntent().getIntExtra(EXTRA_THROTTLE_CODE, -1);
		
		if(throttleNumber!=-1){
			initialiseUi(throttleNumber);
		}
		else{
			exitWithErrorMessage(R.string.error_throttle_number_invalid);
		}
	}

	@TargetApi(19)
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
	    super.onWindowFocusChanged(hasFocus);
	    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
	        if (hasFocus) {
	        	int uiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN;
	        	if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT){
	        		uiVisibility|= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
	        	}
	            getWindow().getDecorView().setSystemUiVisibility(uiVisibility);
	        }
	    }
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_landscape_throttle, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		switch(item.getItemId()){
		case R.id.action_toggle_turnouts_drawer:
			toggleTurnoutsDrawer();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void toggleTurnoutsDrawer() {
		turnoutsDrawerVisible = !turnoutsDrawerVisible;
		if(turnoutsDrawerVisible){
			showTurnoutsDrawer();
		}
		else{
			hideTurnoutsDrawer();
		}
	}

	private void showTurnoutsDrawer() {
		turnoutsDrawer.startAnimation(slideInFromLeft);
		turnoutsDrawer.setVisibility(View.VISIBLE);
	}

	private void hideTurnoutsDrawer() {
		turnoutsDrawer.startAnimation(slideOutToLeft);
		turnoutsDrawer.setVisibility(View.INVISIBLE);
	}

	private void initialiseUi(int throttleNumber) {
		turnoutsDrawer = (RelativeLayout) findViewById(R.id.activity_landscape_throttle_turnouts);
		
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		throttleFragment = new FragmentThrottleLandscapePhone();
		throttleFragment.setThrottleNumber(throttleNumber);
		transaction.replace(R.id.activity_landscape_throttle_throttle, throttleFragment);
		transaction.commit();
		
		transaction = getSupportFragmentManager().beginTransaction();
		turnoutsListFragment = new FragmentTurnoutsList();
		transaction.replace(R.id.activity_landscape_throttle_turnouts, turnoutsListFragment);
		transaction.commit();
		
		slideInFromLeft = AnimationUtils.loadAnimation(this, R.anim.slide_in_from_left);
		slideOutToLeft = AnimationUtils.loadAnimation(this, R.anim.slide_out_to_left);
	}

	private void exitWithErrorMessage(int messageResId) {
		Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
		exit();
	}

	private void exit() {
		//TODO Transition

		finish();
	}
	
	@Override
	protected void onDestroy(){
		super.onDestroy();
	}
	
	
	
	@Override
	public void onBackPressed() {
		exit();
	}
/*
	@Override
	public void finish(){
		Log.d(TAG, "FINISH");
		Intent returnIntent = new Intent();
		returnIntent.putExtra(EXTRA_RETURNED_LOCOMOTIVE, throttleFragment.getLocomotive());
		setResult(RESULT_OK, returnIntent);
		super.finish();
	}
*/
	

	
/*
	private void setLocomotive(Locomotive locomotive) {
		if(locomotive!=null){
			getActionBar().setTitle(locomotive.getNumber());
			getActionBar().setSubtitle(locomotive.getModel());
		}
		else{
			getActionBar().setTitle(R.string.no_locomotive_selected);
			getActionBar().setSubtitle("");
		}
	}
*/
	
}