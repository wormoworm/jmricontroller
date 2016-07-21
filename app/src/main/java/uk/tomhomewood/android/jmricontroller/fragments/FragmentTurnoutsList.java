package uk.tomhomewood.android.jmricontroller.fragments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import uk.tomhomewood.android.jmricontroller.Database;
import uk.tomhomewood.android.jmricontroller.R;
import uk.tomhomewood.android.jmricontroller.Turnout;
import uk.tomhomewood.android.jmricontroller.customviews.TurnoutIndicatorView;
import uk.tomhomewood.android.jmricontroller.network.WiThrottleCommandClientTurnouts;
import uk.tomhomewood.android.jmricontroller.network.WiThrottleCommandClientTurnouts.TurnoutEventListener;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class FragmentTurnoutsList extends JmriControllerFragment implements OnItemClickListener, TurnoutEventListener{
	private final String TAG = "FragmentTurnoutsList";
	
	private RelativeLayout fragmentLayout;
	private FragmentActivity parentActivity;
	
	private Database database;

	int colourNotSelected, colourSelected;
	
	@SuppressWarnings("rawtypes")
	private AdapterView turnoutsList;
	private TurnoutsAdapter turnoutsAdapter;
	
	private WiThrottleCommandClientTurnouts turnoutsCommandClient;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
		super.onCreateView(inflater, container, savedInstanceState);
		
		parentActivity = (FragmentActivity) super.getActivity();
		
		turnoutsCommandClient = getControl().getTurnoutsCommandClient();
		turnoutsCommandClient.addTurnoutEventListener(this);        //TODO NPE here
		
		// Inflate the layout for this fragment
		fragmentLayout = (RelativeLayout) inflater.inflate(R.layout.fragment_turnouts_list, container, false);
		
		colourNotSelected = getResources().getColor(R.color._EEEEEE);
		colourSelected = getResources().getColor(R.color._FFFFFF);
		
		database = new Database(parentActivity);
		
		if(turnoutsCommandClient.isReady()){
			initialiseUi();
		}
		
		return fragmentLayout;
	}

	@Override
	public void ready() {
		initialiseUi();
	}

	@SuppressWarnings("unchecked")
	private void initialiseUi(){
		turnoutsList = (AdapterView<?>) fragmentLayout.findViewById(R.id.fragment_turnouts_list_listview);
		turnoutsList.setOnItemClickListener(this);
		
		turnoutsAdapter = new TurnoutsAdapter(parentActivity, database.getTurnouts());
		turnoutsList.setAdapter(turnoutsAdapter);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		turnoutsCommandClient.removeTurnoutEventListener(this);
	}

	/**
	 * An inner class that represents a custom list adapter that is used to show a list of turnouts
	 */
	public class TurnoutsAdapter extends BaseAdapter {
		private Context context;
		private ArrayList<Turnout> turnouts;
        private Turnout turnoutChanged;
		//private HashMap<String, TurnoutIndicatorView> turnoutIndicators;
		
		/**
		 * Constructor
		 * @param context			The context of the class that instantiated this adapter
		 */
		TurnoutsAdapter (Context context, ArrayList<Turnout> turnouts){
			this.context = context;
			this.turnouts = turnouts;
			//turnoutIndicators = new HashMap<String, TurnoutIndicatorView>();
			refresh();
		}
		
		private void refresh() {
			notifyDataSetChanged();
		}

		public void updateTurnout(Turnout turnout) {
			Turnout existingTurnout = getTurnoutByAddress(turnout.getAddress());
			if(existingTurnout!=null){
				existingTurnout.setState(turnout.getState());
                turnoutChanged = existingTurnout;
                //Log.d(TAG, "Turnout changed: "+turnoutChanged.toString());
				refresh();
			}
		}

		public int getCount() {
			return turnouts.size();
		}

		public Turnout getItem(int position) {
			if(position<turnouts.size()){
				return turnouts.get(position);
			}
			else return null;
		}
		
		public Turnout getTurnoutByAddress(String address){
			Iterator<Turnout> iterator = turnouts.iterator();
			Turnout turnout = null, temp;
			boolean turnoutFound = false;
			while(iterator.hasNext() && !turnoutFound){
				temp = iterator.next();
				if(temp.getAddress().equals(address)){
					turnout = temp;
					turnoutFound = true;
				}
			}
			return turnout;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View view, ViewGroup parent) {
			Turnout turnout = getItem(position);

			ViewHolder viewHolder;
			if (view==null){
                //Log.d(TAG, "Inflating for turnout: "+turnout.toString());
				LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = vi.inflate(R.layout.fragment_turnouts_list_item, parent, false);
				viewHolder = new ViewHolder();
				viewHolder.layout = (RelativeLayout) view.findViewById(R.id.fragment_turnouts_list_item_layout);
				viewHolder.testName = (TextView) view.findViewById(R.id.fragment_turnouts_list_item_name);
                viewHolder.indicator = (TurnoutIndicatorView) view.findViewById(R.id.fragment_turnouts_list_item_indicator);
				view.setTag(viewHolder);

			}
			else{
				viewHolder = (ViewHolder) view.getTag();
			}
			viewHolder.testName.setText(turnout.getName());
            boolean thisTurnoutChanged = turnoutChanged!=null && turnout.equals(turnoutChanged);
            //Log.d(TAG, "getView, turnout: "+turnout.toString()+", changed: "+thisTurnoutChanged);
			viewHolder.indicator.setState(turnout.getState(), true);
            if(thisTurnoutChanged){
                turnoutChanged = null;
            }
			
			return view;
		}
		
		class ViewHolder{
			public RelativeLayout layout;
			public TextView testName;
			public TurnoutIndicatorView indicator;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> adapter, View item, int position, long itemId) {
		Turnout turnout = turnoutsAdapter.getItem(position);
		turnoutsCommandClient.toggleTurnout(turnout, null);
	}

	@Override
	public void turnoutStateChanged(Turnout turnout) {
		turnoutsAdapter.updateTurnout(turnout);
	}
}