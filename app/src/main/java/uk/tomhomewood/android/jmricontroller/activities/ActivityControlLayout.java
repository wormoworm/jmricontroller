package uk.tomhomewood.android.jmricontroller.activities;

import uk.tomhomewood.android.jmricontroller.R;
import uk.tomhomewood.android.jmricontroller.Utils;
import uk.tomhomewood.android.jmricontroller.api.Control;
import uk.tomhomewood.android.jmricontroller.fragments.FragmentThrottle;
import uk.tomhomewood.android.jmricontroller.fragments.FragmentThrottleStandard;
import uk.tomhomewood.android.jmricontroller.fragments.FragmentTurnoutsList;
import uk.tomhomewood.android.jmricontroller.network.NetworkServiceDescriptor;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.WindowManager;

public class ActivityControlLayout extends BaseControlActivity {
	private final String TAG = "ActivityControlLayout";

	//public static final String EXTRA_SERVICE_DESCRIPTOR = "serviceDescriptor";
//    public static final String EXTRA_LAYOUT_NAME = "serviceDescriptor";
//	public static final String EXTRA_MANUAL_ADDRESS = "manualAddress";

	private static final int N_THROTTLES_IN_PAGER = 3;

	private final int THROTTLE_PAGE_DIVIDER_WIDTH_DP = 1;

	private final int ACTION_CHOOSE_LOCOMOTIVE = 1;

	private FragmentManager fragmentManager;
	private ViewPager throttlePager;

	private Bundle savedInstanceState;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control_layout);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        NetworkServiceDescriptor connectionNetworkServiceDescriptor = control.getConnectionNetworkServiceDescriptor();
        if(connectionNetworkServiceDescriptor==null){
            finishAndExit(RESULT_CANCELED, "Connection is null");
        }
        String layoutName = control.getConnectionNetworkServiceDescriptor().getHostName();
        if(layoutName!=null){
            getSupportActionBar().setTitle(layoutName);
        }

		this.savedInstanceState = savedInstanceState;

		fragmentManager = getSupportFragmentManager();

		//initialiseTurnoutFragment();
		//initialiseThrottles();

		overridePendingTransition(R.anim.slide_in_from_right, R.anim.stay_still);
	}

	private void initialiseThrottlePager() {
		throttlePager = (ViewPager) findViewById(R.id.activity_control_layout_fragment_throttle);
		throttlePager.setOffscreenPageLimit(2);
		throttlePager.setAdapter(new ThrottlesPagerAdapter(this, fragmentManager, N_THROTTLES_IN_PAGER));
		throttlePager.setPageMargin(Utils.convertDpToPixels(THROTTLE_PAGE_DIVIDER_WIDTH_DP, this));
		throttlePager.setPageMarginDrawable(R.color.primary);
	}
/*
    private void initialiseThrottle(int throttleNumber, Bundle savedInstanceState) {
        FragmentThrottle throttleFragment;
        if(savedInstanceState!=null){
            throttleFragment = (FragmentThrottle) getSupportFragmentManager().findFragmentById(getContainerIdForThrottleNumber(throttleNumber));
            //Log.d(TAG, "About to set throttle number: "+throttleNumber);
            throttleFragment.setThrottleNumber(throttleNumber);
        }
        else{
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            throttleFragment = new FragmentThrottleStandard();
            throttleFragment.setThrottleNumber(throttleNumber);
            transaction.replace(getContainerIdForThrottleNumber(throttleNumber), throttleFragment);
            transaction.commit();
        }
    }
*/
    private void initialiseThrottle(int throttleNumber) {
        FragmentThrottle throttleFragment = (FragmentThrottle) getSupportFragmentManager().findFragmentById(getContainerIdForThrottleNumber(throttleNumber));
        throttleFragment.setThrottleNumber(throttleNumber);
    }

	private int getContainerIdForThrottleNumber(int throttleNumber) {
		switch(throttleNumber){
		default:
		case 0:
			return R.id.activity_control_layout_fragment_throttle_1;
		case 1:
			return R.id.activity_control_layout_fragment_throttle_2;
		case 2:
			return R.id.activity_control_layout_fragment_throttle_3;
		}
	}

	@Override
	public void turnoutsListAvailable() {
		Log.d(TAG, "turnoutsListAvailable()");
	}

	@Override
	public void locomotiveListAvailable() {
		initialiseThrottles();
	}
/*
	private void initialiseTurnoutFragment() {
		FragmentTurnoutsList turnoutsListFragment;
		if(savedInstanceState!=null){
			turnoutsListFragment = (FragmentTurnoutsList) getSupportFragmentManager().findFragmentById(R.id.activity_control_layout_fragment_turnouts);
		}
		else{
			FragmentTransaction transaction = fragmentManager.beginTransaction();
			turnoutsListFragment = new FragmentTurnoutsList();
			transaction.replace(R.id.activity_control_layout_fragment_turnouts, turnoutsListFragment);
			transaction.commit();
		}
	}
*/
	private void initialiseThrottles() {
        Log.d(TAG, "initialiseThrottles()");
		int screenType = getScreenType();
		if(screenType==SCREEN_TYPE_PHONE){
			initialiseThrottlePager();
		}
		else if(screenType==SCREEN_TYPE_TABLET_SMALL){
			initialiseThrottle(0);
			initialiseThrottle(1);
		}	
		else if(screenType==SCREEN_TYPE_TABLET_LARGE){
			initialiseThrottle(0);
			initialiseThrottle(1);
			initialiseThrottle(2);
		}
	}

    @Override
	protected void onDestroy(){
		super.onDestroy();
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	public void launchChooseLocomotiveActivity(boolean multiSelect) {
		Intent launchIntent = new Intent(this, ActivityChooseLocomotive.class);
		launchIntent.putExtra(ActivityChooseLocomotive.EXTRA_MULTI_SELECT, multiSelect);
		startActivityForResult(launchIntent, ACTION_CHOOSE_LOCOMOTIVE);
	}

	private void finishAndExit(int resultCode, String message) {
		if(resultCode==RESULT_CANCELED){
			Log.e(TAG, message);
		}
		setResult(resultCode);
		finish();
	}

	@Override
	public void finish() {
		super.finish();
		control.disconnectFromLayout();
		overridePendingTransition(R.anim.stay_still, R.anim.slide_out_to_right);
	}

	@Override
	public void onBackPressed(){
		finishAndExit(RESULT_OK, "Exiting from back press");
	}

	public class ThrottlesPagerAdapter extends FragmentStatePagerAdapter {
		private int nThrottles;

        private Activity activity;

		public ThrottlesPagerAdapter(Activity activity, FragmentManager fragmentManager, int nThrottles) {
			super(fragmentManager);
            this.activity = activity;
			this.nThrottles = nThrottles;
		}

		@Override
		public int getCount() {
			return nThrottles;
		}

		@Override
		public FragmentThrottle getItem(int position) {
			FragmentThrottle fragment = new FragmentThrottleStandard();
            fragment.onAttach(activity);
			fragment.setThrottleNumber(position);
			return fragment;
		}
	}
}