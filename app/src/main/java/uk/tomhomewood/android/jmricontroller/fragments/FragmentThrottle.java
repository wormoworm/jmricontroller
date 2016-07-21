package uk.tomhomewood.android.jmricontroller.fragments;

import java.util.ArrayList;

import org.json.JSONObject;

import uk.tomhomewood.android.jmricontroller.BitmapUtils;
import uk.tomhomewood.android.jmricontroller.Function;
import uk.tomhomewood.android.jmricontroller.ImageCache;
import uk.tomhomewood.android.jmricontroller.ImageLoader;
import uk.tomhomewood.android.jmricontroller.Locomotive;
import uk.tomhomewood.android.jmricontroller.R;
import uk.tomhomewood.android.jmricontroller.activities.ActivityChooseLocomotive;
import uk.tomhomewood.android.jmricontroller.activities.ActivityViewLocomotiveInfo;
import uk.tomhomewood.android.jmricontroller.customviews.DirectionControlView;
import uk.tomhomewood.android.jmricontroller.customviews.FunctionsAdapter;
import uk.tomhomewood.android.jmricontroller.customviews.OldSpeedControlView;
import uk.tomhomewood.android.jmricontroller.customviews.OldSpeedControlView.SpeedControlListener;
import uk.tomhomewood.android.jmricontroller.customviews.TouchGridView;
import uk.tomhomewood.android.jmricontroller.customviews.DirectionControlView.DirectionControlListener;
import uk.tomhomewood.android.jmricontroller.customviews.TouchGridView.TouchGridListener;
import uk.tomhomewood.android.jmricontroller.dialogs.DialogThrottleLocomotiveInfo;
import uk.tomhomewood.android.jmricontroller.network.WiThrottleCommandClientThrottle;
import uk.tomhomewood.android.jmricontroller.network.WiThrottleCommandClientThrottle.Direction;
import uk.tomhomewood.android.jmricontroller.network.WiThrottleCommandClientThrottle.ThrottleEventListener;
import uk.tomhomewood.android.jmricontroller.settings.Settings;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ViewSwitcher;

public abstract class FragmentThrottle extends JmriControllerFragment implements ThrottleEventListener, SpeedControlListener, DirectionControlListener, OnClickListener, TouchGridListener {
	private String TAG = "FragmentThrottle";

	private final String SAVED_STATE_THROTTLE_NUMBER = "throttleNumber";
	//private final String SAVED_STATE_LOCOMOTIVE = "locomotive";

	private final int ACTION_CHOOSE_LOCOMOTIVE = 1;

	private final int NUM_FUNCTIONS = 29;

	private RelativeLayout fragmentLayout;
//	protected FragmentActivity getParentActivity();

	private ViewSwitcher viewSwitcher;
//	private RelativeLayout noLocoSelected;
	private Button noLocoSelectedButton;

    private Button locoInfo, changeLoco;
	private ImageView locomotiveImage;
	private OldSpeedControlView speedControl;
	private DirectionControlView directionControl;
	private TouchGridView functionsGrid;
	private FunctionsAdapter functionsAdapter;

	protected WiThrottleCommandClientThrottle throttleCommandClient;
	private int throttleNumber;
	
	private ArrayList<Function> functionsList;

    private ImageLoader imageLoader;
	
	private SharedPreferences preferences;

    private Animation locoImageAnimation;
	
//	private Locomotive pendingLocomotive;

	public FragmentThrottle setThrottleNumber(int throttleNumber){
		this.throttleNumber = throttleNumber;
		TAG+= "_"+throttleNumber;

        throttleCommandClient = getControl().getThrottleCommandClient(throttleNumber);
        throttleCommandClient.setStatusPollingEnabled(false);
        throttleCommandClient.addThrottleEventListener(this);
        //Check for fragment layout being null, if it is not then we know that onCreateView() has been called
        if(fragmentLayout!=null && throttleCommandClient.isReady()){
            ready();
        }
		return this;
	}

    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		preferences = PreferenceManager.getDefaultSharedPreferences(getParentActivity());

        imageLoader = new ImageLoader();

        locoImageAnimation = AnimationUtils.loadAnimation(getParentActivity(), R.anim.fade_in);

		// Inflate the layout for this fragment
		fragmentLayout = (RelativeLayout) inflater.inflate(getLayoutResId(), container, false);

		viewSwitcher = (ViewSwitcher) fragmentLayout.findViewById(R.id.fragment_throttle_view_switcher);

		noLocoSelectedButton = (Button) fragmentLayout.findViewById(R.id.fragment_throttle_no_loco_selected_button);

		locomotiveImage = (ImageView) fragmentLayout.findViewById(R.id.fragment_throttle_loco_image);

        locoInfo = (Button) fragmentLayout.findViewById(R.id.fragment_throttle_loco_more_info);
        changeLoco = (Button) fragmentLayout.findViewById(R.id.fragment_throttle_loco_change);
		
		speedControl = (OldSpeedControlView) fragmentLayout.findViewById(R.id.fragment_throttle_speed_control);
		directionControl = (DirectionControlView) fragmentLayout.findViewById(R.id.fragment_throttle_direction_control);
		functionsGrid = (TouchGridView) fragmentLayout.findViewById(R.id.fragment_throttle_functions_grid);
		
		viewSwitcher.setDisplayedChild(1);

        locomotiveImage.setOnClickListener(this);
        locoInfo.setOnClickListener(this);
        changeLoco.setOnClickListener(this);
		noLocoSelectedButton.setOnClickListener(this);

		speedControl.setSpeedControlListener(this);
		directionControl.setDirectionControlListener(this);

		directionControl.setStopped(false);
		
		functionsGrid.setTouchGridListener(this);
		
		return fragmentLayout;
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
        if(savedInstanceState!=null){
            if(savedInstanceState.containsKey(SAVED_STATE_THROTTLE_NUMBER)){
                int throttleNumber = savedInstanceState.getInt(SAVED_STATE_THROTTLE_NUMBER);
                setThrottleNumber(throttleNumber);
            }
        }
        if(throttleCommandClient!=null && throttleCommandClient.isReady()){
            ready();
        }
	}

    @Override
    public void onDestroy() {
        throttleCommandClient.removeThrottleEventListener(this);
        super.onDestroy();
    }
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_throttle, menu);
		if((throttleCommandClient!=null && !throttleCommandClient.hasAcquiredLocomotive()) || !preferences.getBoolean(Settings.PREFS_KEY_SHOW_EMERGENCY_STOP_BUTTON, false)){
//			menu.removeItem(R.id.action_emergency_stop);
		}
	}

	@Override
	public void ready() {
		initialiseUi();
	}

	private void initialiseUi() {
		if(throttleCommandClient.hasAcquiredLocomotive()){
			locomotiveAcquired(throttleCommandClient.getLocomotive());
		}
		else{
			noLocoSelected();
		}
	}

	protected abstract int getLayoutResId();

    @Override
	public void onResume() {
		super.onResume();
		
	}

	@Override
	public void onPause() {
		super.onPause();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState){
		outState.putInt(SAVED_STATE_THROTTLE_NUMBER, throttleNumber);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case ACTION_CHOOSE_LOCOMOTIVE:
			if(resultCode==Activity.RESULT_OK && data!=null){
				Locomotive locomotive = (Locomotive) data.getSerializableExtra(ActivityChooseLocomotive.EXTRA_LOCOMOTIVE);
				if(locomotive!=null){
					setLocomotive(locomotive);
				}
			}
			break;
		default:
			super.onActivityResult(requestCode, resultCode, data);
			break;
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		getParentActivity().invalidateOptionsMenu();
		if(key.equals(Settings.PREFS_KEY_SHOW_ALL_FUNCTIONS)){
			if(hasLocomotive()){
				loadLocomotiveFunctions(throttleCommandClient.getLocomotive());
			}
		}
		super.onSharedPreferenceChanged(sharedPreferences, key);
	}

	public void setLocomotive(Locomotive locomotive) {
		throttleCommandClient.setLocomotive(locomotive, false);
	}

	public Locomotive getLocomotive() {
		return throttleCommandClient.getLocomotive();
	}
	
	public boolean hasLocomotive(){
		return getLocomotive()!=null;
	}

	private void launchChooseLocomotiveActivity(boolean multiSelect) {
		Intent launchIntent = new Intent(getParentActivity(), ActivityChooseLocomotive.class);
		launchIntent.putExtra(ActivityChooseLocomotive.EXTRA_MULTI_SELECT, multiSelect);
		startActivityForResult(launchIntent, ACTION_CHOOSE_LOCOMOTIVE);
	}

	protected void loadLocomotive(Locomotive locomotive) {
		speedControl.setMaxSpeed(locomotive.getMaxSpeed());
		if(locomotive.hasImage()){
			loadLocomotiveImage(locomotive, locomotiveImage);
		}
        locomotiveImage.setVisibility(locomotive.hasImage()? View.VISIBLE : View.INVISIBLE);
		speedControl.setSpeed(throttleCommandClient.getCurrentLocomotiveSpeed());
		directionControl.setDirection(throttleCommandClient.getCurrentLocomotiveDirection(), false);
		loadLocomotiveFunctions(locomotive);
	}

    private void loadLocomotiveImage(Locomotive locomotive, ImageView imageView) {
        imageLoader.loadImage(locomotive.getImagePath(), imageView, locoImageAnimation);
    }

	private void loadLocomotiveFunctions(Locomotive locomotive) {
		functionsList = new ArrayList<Function>();
		for(int i=0; i<NUM_FUNCTIONS; i++){
			Function function = new Function(i, "F"+i, true);
			JSONObject functionJson = locomotive.getFunctionJson(i);
			if(functionJson!=null){		//Means there is some custom data available for this particular function
				function.setName(functionJson.optString(Function.KEY_NAME, "F"+i));
				function.setLockable(functionJson.optBoolean(Function.KEY_LOCKABLE, true));
				functionsList.add(function);
			}
			else if(shouldShowAllFunctions()){
				functionsList.add(function);
			}
		}
		functionsAdapter = new FunctionsAdapter(getParentActivity(), functionsList);
		functionsGrid.setAdapter(functionsAdapter);
	}

	private boolean shouldShowAllFunctions() {
		return getPreferences().getBoolean(Settings.PREFS_KEY_SHOW_ALL_FUNCTIONS, false);
	}

	protected void locoSelected() {
		getParentActivity().invalidateOptionsMenu();
		viewSwitcher.setDisplayedChild(0);
	}

	protected void noLocoSelected() {
		getParentActivity().invalidateOptionsMenu();
		viewSwitcher.setDisplayedChild(1);
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()){
            case R.id.fragment_throttle_no_loco_selected_button:
            case R.id.fragment_throttle_loco_change:
                launchChooseLocomotiveActivity(false);
                break;
            case R.id.fragment_throttle_loco_more_info:
            case R.id.fragment_throttle_loco_image:
                ActivityViewLocomotiveInfo.launch(getParentActivity(), getLocomotive());
                break;
            default:
			break;
		}
	}
	
	@Override
	public void speedChanged(int speed) {
		throttleCommandClient.setSpeed(speed);
	}

	@Override
	public void directionChanged(Direction direction) {
		speedControl.setSliderEnabled(true);
		directionControl.setDirection(direction, true);
		throttleCommandClient.setDirection(direction);
	}

	@Override
	public void stopped() {
		speedControl.setSpeed(0);
		speedControl.setSliderEnabled(false);
		throttleCommandClient.setSpeed(0);
	}

	@Override
	public void onTouchDown(int position) {
		Function function = functionsAdapter.getItem(position);
		if(function!=null){
			throttleCommandClient.turnFunctionOn(function);
		}
	}

	@Override
	public void onTouchUp(int position) {
		Function function = functionsAdapter.getItem(position);
		if(function!=null){
			throttleCommandClient.turnFunctionOff(function);
		}
	}

	@Override
	public void locomotiveAcquired(Locomotive locomotive) {
		loadLocomotive(locomotive);
		locoSelected();
	}

	@Override
	public void locomotiveReleased() {
		noLocoSelected();
	}

	@Override
	public void locomotiveSpeedChanged(Locomotive locomotive, int speed) {
		speedControl.setSpeed(speed);
	}

	@Override
	public void locomotiveEmergencyStop(Locomotive locomotive) {
		stopped();
	}

	@Override
	public void locomotiveDirectionChanged(Locomotive locomotive, Direction direction, Direction previousDirection) {
		directionControl.setDirection(direction, false);
	}

	@Override
	public void functionTurnedOn(int functionNumber) {
		Function function = functionsAdapter.getFunctionByNumber(functionNumber);
		if(function!=null){
			function.turnOn();
			functionsAdapter.refresh();
		}
	}

	@Override
	public void functionTurnedOff(int functionNumber) {
		Function function = functionsAdapter.getFunctionByNumber(functionNumber);
		if(function!=null){
			function.turnOff();
			functionsAdapter.refresh();
		}
	}
}