package uk.tomhomewood.android.jmricontroller.customviews;

import java.util.ArrayList;
import java.util.Iterator;

import uk.tomhomewood.android.jmricontroller.Function;
import uk.tomhomewood.android.jmricontroller.R;
import android.content.Context;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class FunctionsAdapter extends BaseAdapter{
//	private final String TAG = "FunctionsAdapter";

	private Context context;
	private ArrayList<Function> functions;
	private SparseIntArray functionNumbersToIndexes;

	public FunctionsAdapter(Context context, ArrayList<Function> functionsList){
		this.context = context;
		setFunctions(functionsList);
	}

	public Function getFunctionByNumber(int functionNumber) {
		Integer functionIndex = getFunctionIndexByNumber(functionNumber);
		if(functionIndex!=null){
			return getItem(functionIndex);
		}
		else{
			return null;
		}
	}

	private Integer getFunctionIndexByNumber(int functionNumber) {
		return functionNumbersToIndexes.get(functionNumber);
	}

	public void refresh() {
		notifyDataSetChanged();
	}

	private void setFunctions(ArrayList<Function> functionsList) {
		functions = functionsList;
		buildFunctionIndex(functionsList);
	}

	private void buildFunctionIndex(ArrayList<Function> functionsList) {
		functionNumbersToIndexes = new SparseIntArray();
		Iterator<Function> iterator = functionsList.iterator();
		Function function;
		int index = 0;
		while(iterator.hasNext()){
			function = iterator.next();
			functionNumbersToIndexes.put(function.getNumber(), index);
			index++;
		}
	}

	public void setFunctionsList(ArrayList<Function> functionsList) {
		setFunctions(functionsList);
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return functions.size();
	}

	@Override
	public Function getItem(int position) {
		return functions.get(position);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View view, ViewGroup parent) {
		ViewHolder viewHolder;
		if(view==null){
			viewHolder = new ViewHolder();
			LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = vi.inflate(R.layout.fragment_throttle_functions_grid_item, parent, false);
			viewHolder.label = (TextView) view.findViewById(R.id.fragment_throttle_functions_grid_item_label);
			//viewHolder.indicator = (TextView) view.findViewById(R.id.fragment_throttle_functions_grid_item_indicator);
			view.setTag(viewHolder);
		}
		else{
			viewHolder = (ViewHolder) view.getTag();
		}
		Function function = getItem(position);
		viewHolder.label.setText(function.getName());
		viewHolder.label.setSelected(function.isTurnedOn());
		//viewHolder.indicator.setSelected(function.isTurnedOn());
		return view;
	}

	class ViewHolder{
		public TextView label;
		//public TextView indicator;
	}
}