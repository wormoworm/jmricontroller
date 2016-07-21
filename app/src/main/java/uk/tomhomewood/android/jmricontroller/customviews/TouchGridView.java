package uk.tomhomewood.android.jmricontroller.customviews;

import uk.tomhomewood.android.jmricontroller.Utils;
import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.GridView;

public class TouchGridView extends GridView{
	//private final String TAG = "TouchGridView";
	
	private final int TOUCH_STATE_WAITING_FOR_DOWN = 1;
	private final int TOUCH_STATE_DOWN = 2;
	private final int TOUCH_STATE_UP = 3;
	private final int TOUCH_STATE_SCROLL = 4;
	
	private final float TOUCH_SLOP_DP = 3f;
	private final int BUFFER_FRAMES = 15;
	
	private ViewConfiguration vc;
	private int touchSlop;
	private int touchEventX, touchEventY;
	
	private int touchState = TOUCH_STATE_UP;
	private int potentialDownEvents;
	private long downEventTime;

	private Handler handler;
	
	private TouchGridListener touchGridListener;
	
	public TouchGridView(Context context) {
		super(context);
	}

	public TouchGridView(Context context, AttributeSet attrs) {
		super(context, attrs);
		vc = ViewConfiguration.get(context);
		touchSlop = vc.getScaledTouchSlop();
		touchSlop = Utils.convertDpToPixels(TOUCH_SLOP_DP, context);
		potentialDownEvents = 0;
		handler = new Handler();
	}
	
	public void setTouchGridListener(TouchGridListener touchGridListener){
		this.touchGridListener = touchGridListener;
	}

	@Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
		return true;
	}


	@Override
	public boolean onTouchEvent(MotionEvent event){
		//Log.d(TAG, "Time: "+event.getEventTime());
		int eventAction = event.getAction();
		switch (eventAction) {
		case MotionEvent.ACTION_DOWN:
			if(touchState!=TOUCH_STATE_DOWN){
				touchEventX = (int) event.getX();
				touchEventY = (int) event.getY();
				postDelayedDownMessage();
				potentialDownEvents = 0;
				return super.onTouchEvent(event);
			}
		case MotionEvent.ACTION_MOVE:
			if(touchState!=TOUCH_STATE_DOWN){
				int distanceFromLastEvent = distanceBetweenEvents(touchEventX, touchEventY, (int) event.getX(), (int) event.getY());
				//Log.d(TAG, "Distance from last: "+distanceFromLastEvent);
				if(distanceFromLastEvent>touchSlop){
					removeDelayedDownMessage();
					touchState = TOUCH_STATE_SCROLL;
					return super.onTouchEvent(event);
				}
				else{
					potentialDownEvents++;
					if(potentialDownEvents<BUFFER_FRAMES){
						removeDelayedDownMessage();
						postDelayedDownMessage();
					}
					return super.onTouchEvent(event);
				}
			}
			return true;
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			if(touchState==TOUCH_STATE_DOWN){
				upEvent();
			}
			else if(touchState==TOUCH_STATE_WAITING_FOR_DOWN){
				removeDelayedDownMessage();
				downEvent();
				postDelayedUpMessage(System.currentTimeMillis() - downEventTime);
			}
			return super.onTouchEvent(event);
		default:
			return true;
		}
	}

	private int distanceBetweenEvents(int x1, int y1, int x2, int y2) {
		//Log.d(TAG, "1 Y: "+y1+", 2 Y: "+y2);
		int dX = x2 - x1;
		int dY = y2 - y1;
		int distance = (int) Math.round(Math.sqrt((dX * dX) + (dY * dY)));
		return distance;
	}

	private void downEvent() {
		touchState = TOUCH_STATE_DOWN;
		if(touchGridListener!=null){
			int position = pointToPosition(touchEventX, touchEventY);
			if(position!=INVALID_POSITION){
				touchGridListener.onTouchDown(position);
			}
		}
	}

	private void upEvent() {
		touchState = TOUCH_STATE_UP;
		if(touchGridListener!=null){
			int position = pointToPosition(touchEventX, touchEventY);
			if(position!=INVALID_POSITION){
				touchGridListener.onTouchUp(position);
			}
		}
	}

	private void postDelayedDownMessage() {
		downEventTime = System.currentTimeMillis();
		handler.postDelayed(delayedDownMessage, (BUFFER_FRAMES - 1) * 18);
		touchState = TOUCH_STATE_WAITING_FOR_DOWN;
	}

	private void removeDelayedDownMessage() {
		handler.removeCallbacks(delayedDownMessage);
	}

	private Runnable delayedDownMessage = new Runnable() {
		@Override
		public void run() {
			downEvent();
		}
	};

	private void postDelayedUpMessage(long delay) {
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				upEvent();
			}
		}, delay);
	}
	
	public interface TouchGridListener{
		
		public void onTouchDown(int position);
		
		public void onTouchUp(int position);
	}
}