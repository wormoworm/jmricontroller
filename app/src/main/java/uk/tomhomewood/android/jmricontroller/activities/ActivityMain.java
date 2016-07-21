package uk.tomhomewood.android.jmricontroller.activities;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import uk.tomhomewood.android.jmricontroller.FileUtils;
import uk.tomhomewood.android.jmricontroller.R;
import uk.tomhomewood.android.jmricontroller.RecentConnectionsClient;
import uk.tomhomewood.android.jmricontroller.Utils;
import uk.tomhomewood.android.jmricontroller.api.ConnectToLayoutListener;
import uk.tomhomewood.android.jmricontroller.api.Control;
import uk.tomhomewood.android.jmricontroller.api.LayoutScanListener;
import uk.tomhomewood.android.jmricontroller.network.NetworkServiceDescriptor;
import uk.tomhomewood.android.jmricontroller.settings.Settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.melnykov.fab.FloatingActionButton;

public class ActivityMain extends BaseActivity implements OnItemClickListener, OnClickListener, OnEditorActionListener, LayoutScanListener {
	private final String TAG = "ActivityMain";

	private final int REQUEST_START_CONTROL_ACTIVITY = 1;

	private final int CHILD_LAYOUTS = 0;
	private final int CHILD_MANUAL = 1;

	private Control control;

    private TextView autoHeading;

    private FloatingActionButton buttonManualConnect;
	private Button buttonConnect;

    private RelativeLayout manualConnect;

	private ServiceDescriptorsAdapter serviceDescriptorsAdapter;
	private ListView serviceDescriptorsList;

	private AutoCompleteTextView manualConnectAddress, manualConnectPort;

	private SharedPreferences preferences;

	private RecentConnectionsClient recentConnectionsClient;

	private boolean isFirstDiscoveredService;

	private boolean shouldAutoConnect;

	private String throttleName;

    private Animation slideUp, slideDown;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        getSupportActionBar().setTitle(R.string.choose_layout);

		control = Control.getInstance(this);

		String random = Utils.generateSHA1(new Random().nextInt()+""+System.nanoTime()).substring(0, 4);
		throttleName = getString(R.string.app_name)+" "+random;

		forceActionBarOverflow();

        slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up_from_bottom);
        slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down_to_bottom);

		preferences = PreferenceManager.getDefaultSharedPreferences(this);

        autoHeading = (TextView) findViewById(R.id.activity_main_layouts_heading_auto);

		serviceDescriptorsList = (ListView) findViewById(R.id.activity_main_services_list);
		serviceDescriptorsAdapter = new ServiceDescriptorsAdapter();
		serviceDescriptorsList.setAdapter(serviceDescriptorsAdapter);
		serviceDescriptorsList.setOnItemClickListener(this);
		serviceDescriptorsList.setEmptyView(getServiceDescriptorsListEmptyView());

        if(getScreenType()==SCREEN_TYPE_PHONE){
            buttonManualConnect = (FloatingActionButton) findViewById(R.id.activity_main_button_manual);
            buttonManualConnect.setOnClickListener(this);
        }

        manualConnect = (RelativeLayout) findViewById(R.id.activity_main_manual_wrapper);
		manualConnectAddress = (AutoCompleteTextView) findViewById(R.id.activity_main_manual_connection_address);
		manualConnectPort = (AutoCompleteTextView) findViewById(R.id.activity_main_manual_connection_port);
		manualConnectPort.setOnEditorActionListener(this);

		buttonConnect = (Button) findViewById(R.id.activity_main_button_connect);
		buttonConnect.setOnClickListener(this);

		recentConnectionsClient = new RecentConnectionsClient(preferences);

		isFirstDiscoveredService = true;
		shouldAutoConnect = true;

		FileUtils.checkFilesystem(this);

        refreshAutoLayoutHeading();
	}

	protected SharedPreferences getPreferences(){
		return preferences;
	}

	private View getServiceDescriptorsListEmptyView() {
		View view = findViewById(R.id.activity_main_services_empty_item_message);
		return view;
	}

	@Override
	protected void onResume() {
		super.onResume();
		control.addLayoutScanListener(this);
		control.startLayoutScan();

		refreshRecentConnections();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode){
		case REQUEST_START_CONTROL_ACTIVITY:
			shouldAutoConnect = false;		//This will prevent an auto connect after returning from the control layout activity
			break;
		default:
			super.onActivityResult(requestCode, resultCode, data);
			break;
		}
	}

	private void refreshRecentConnections() {
		//First, set the values in each box to the IP address and port of the last successfull connection
		String mostRecentAddress = recentConnectionsClient.getMostRecentAddress();
		String mostRecentPort = recentConnectionsClient.getMostRecenPort();
		if(mostRecentAddress!=null){
			manualConnectAddress.setText(mostRecentAddress);
		}
		if(mostRecentAddress!=null){
			manualConnectPort.setText(mostRecentPort);
		}

		//Now, set the adapters for the autocomplete feature for both address and port number
		ArrayList<String> recentAddresses = recentConnectionsClient.getAddresses();
		if(recentAddresses!=null){
			ArrayAdapter<String> addressesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, recentAddresses);
			manualConnectAddress.setAdapter(addressesAdapter);
		}
		ArrayList<String> recentPorts = recentConnectionsClient.getPorts();
		if(recentPorts!=null){
			ArrayAdapter<String> portsAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, recentPorts);
			manualConnectPort.setAdapter(portsAdapter);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		control.stopLayoutScan();
		control.removeLayoutScanListener(this);
	}

	@Override
	protected void onDestroy(){
		super.onDestroy();
/*  TODO Close socket in API?
		if(withrottleSocketClient!=null && withrottleSocketClient.isSocketConnected()){
			withrottleSocketClient.disconnectSocket();
		}
*/
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		switch(item.getItemId()){
		case R.id.action_settings:
			ActivitySettings.launch(this);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}



	private void launchControlLayoutActivity() {
		Intent launchControlLayoutActivityIntent = new Intent(this, ActivityControlLayout.class);
		startActivityForResult(launchControlLayoutActivityIntent, REQUEST_START_CONTROL_ACTIVITY);
	}

	private void attemptManualConnection() {
		try{
			NetworkServiceDescriptor descriptor = new NetworkServiceDescriptor((Inet4Address) Inet4Address.getByName(manualConnectAddress.getText().toString()), Integer.parseInt(manualConnectPort.getText().toString()), null);
			connectToLayout(descriptor);
		}
		catch(Exception e){
		}
	}

    @Override
    public void onBackPressed() {
        if(getScreenType()==SCREEN_TYPE_PHONE && manualConnect.getVisibility()==View.VISIBLE){
            hideManualConnect();
        }
        else{
            super.onBackPressed();
        }
    }

    private void showManualConnect() {
        manualConnect.startAnimation(slideUp);
        manualConnect.setVisibility(View.VISIBLE);
	}

    private void hideManualConnect() {
        manualConnect.startAnimation(slideDown);
        manualConnect.setVisibility(View.GONE);
    }

	private boolean canAutoConnect() {
		return shouldAutoConnect && getPreferences().getBoolean(Settings.PREFS_KEY_AUTO_CONNECT, false);
	}

	@Override
	public void layoutFound(NetworkServiceDescriptor serviceDescriptor) {
		serviceDescriptorsAdapter.add(serviceDescriptor);
		serviceDescriptorsAdapter.notifyDataSetChanged();
		//True if this is the first layout to be found AND auto connect is enabled
		if(isFirstDiscoveredService && canAutoConnect()){
			connectToLayout(serviceDescriptor);
			isFirstDiscoveredService = false;
		}
        refreshAutoLayoutHeading();
	}

	@Override
	public void layoutRemoved(NetworkServiceDescriptor serviceDescriptor) {
		serviceDescriptorsAdapter.remove(serviceDescriptor);
		serviceDescriptorsAdapter.notifyDataSetChanged();
        refreshAutoLayoutHeading();
	}

    private void refreshAutoLayoutHeading() {
        int nLayouts = serviceDescriptorsAdapter.getCount();
        String headingText = nLayouts+" "+getResources().getQuantityString(R.plurals.layouts, nLayouts)+" "+getString(R.string.found);
        autoHeading.setText(headingText);
    }

    @Override
	public void onItemClick(AdapterView<?> adapter, View item, int position, long itemId) {
		NetworkServiceDescriptor serviceDescriptor = serviceDescriptorsAdapter.getItem(position);
		if(serviceDescriptor!=null){
			connectToLayout(serviceDescriptor);
		}
	}

	private void connectToLayout(NetworkServiceDescriptor serviceDescriptor) {
		control.connectToLayout(serviceDescriptor, throttleName, new ConnectToLayoutListener() {

			@Override
			public void connectedToLayout(NetworkServiceDescriptor serviceDescriptor) {
				Log.d(TAG, "Connected");
				recentConnectionsClient.storeAddress(serviceDescriptor.getAddressString());
				recentConnectionsClient.storePort(serviceDescriptor.getPortNumber());
				launchControlLayoutActivity();
			}

			@Override
			public void connectToLayoutFailed() {
				Log.e(TAG, "Could not connect to layout");
			}
		});
	}

	private class ServiceDescriptorsAdapter extends BaseAdapter {
		private ArrayList<NetworkServiceDescriptor> serviceDescriptors;

		private ServiceDescriptorsAdapter(){
			serviceDescriptors = new ArrayList<NetworkServiceDescriptor>();
		}

		public void add(NetworkServiceDescriptor serviceDescriptor) {
			int index = indexOf(serviceDescriptor);
			if(index==-1){			//Only add if it is not already in the list
				serviceDescriptors.add(serviceDescriptor);
			}
			refresh();
		}

		public void remove(NetworkServiceDescriptor serviceDescriptor) {
			int index = indexOf(serviceDescriptor);
			if(index!=-1){
				serviceDescriptors.remove(index);
			}
			refresh();
		}

		private int indexOf(NetworkServiceDescriptor serviceDescriptor){
			int index = -1;
			int i = 0;
			boolean found = false;
			Iterator<NetworkServiceDescriptor> iterator = serviceDescriptors.iterator();
			while(iterator.hasNext() && !found){
				if(iterator.next().equals(serviceDescriptor)){
					index = i;
					found = true;
				}
				i++;
			}
			return index;
		}

		private void refresh() {
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return serviceDescriptors.size();
		}

		@Override
		public NetworkServiceDescriptor getItem(int position) {
			return serviceDescriptors.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			ViewHolder viewHolder;
			if (view == null) {
				viewHolder = new ViewHolder();
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = vi.inflate(R.layout.activity_main_services_list_item, parent, false);
				viewHolder.name = (TextView) view.findViewById(R.id.activity_main_services_list_item_name);
				viewHolder.address = (TextView) view.findViewById(R.id.activity_main_services_list_item_address);
				view.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) view.getTag();
			}
			NetworkServiceDescriptor serviceDescriptor = getItem(position);
			viewHolder.name.setText(serviceDescriptor.getHostName());
			viewHolder.address.setText(serviceDescriptor.getAddressString()+':'+serviceDescriptor.getPortNumber());
			return view;
		}

		private class ViewHolder {
			TextView name, address;
		}
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()){
		default:
		case R.id.activity_main_button_manual:
			showManualConnect();
			break;
		case R.id.activity_main_button_connect:
			attemptManualConnection();
			break;
		}
	}

	@Override
	public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
		if(actionId==EditorInfo.IME_ACTION_GO){
			attemptManualConnection();
		}
		return false;
	}
}