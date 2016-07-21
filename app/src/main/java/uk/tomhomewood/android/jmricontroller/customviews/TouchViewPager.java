package uk.tomhomewood.android.jmricontroller.customviews;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class TouchViewPager extends ViewPager{
	private final String TAG = "CustomViewPager";
	
	public TouchViewPager(Context context) {
		super(context);
	}
	
	public TouchViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	protected boolean canScroll (View view, boolean checkV, int dx, int x, int y){
//		Log.d(TAG, "canScroll, dX: "+dx);
		
			return super.canScroll(view, checkV, dx, x, y);
	}
	
	@Override
	public boolean onInterceptTouchEvent (MotionEvent ev){
//		Log.d(TAG, "onInterceptTouchEvent");
		return super.onInterceptTouchEvent(ev);
	}
}
