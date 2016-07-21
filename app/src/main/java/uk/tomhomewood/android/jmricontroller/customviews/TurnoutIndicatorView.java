package uk.tomhomewood.android.jmricontroller.customviews;

import uk.tomhomewood.android.jmricontroller.R;
import uk.tomhomewood.android.jmricontroller.Turnout;
import uk.tomhomewood.android.jmricontroller.Utils;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Interpolator;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.util.Log;

public class TurnoutIndicatorView extends SmartView implements AnimatorUpdateListener{
	private final String TAG = "TurnoutIndicatorViewNew";
	
	private final long ANIMATION_DURATION_MS = 300;
	private final int LINE_WIDTH_DP = 4;
	
//	private int indicatorHeight;
	private int indicatorAngleUnknown = 90, indicatorAngleThrown = 115, indicatorAngleClosed = 65;
	private int indicatorAngle = indicatorAngleUnknown;
	
	private int lineWidthPx;

	private int colourUnknown, colourKnown;

	private int currentState;

	private Paint paintIndicator;
	
	private ValueAnimator animator;

	public TurnoutIndicatorView(Context context) {
		super(context);
        Log.d(TAG, "Constructor 1: "+hashCode());
	}

	public TurnoutIndicatorView(Context context, AttributeSet attrs) {
		super(context, attrs);
        Log.d(TAG, "Constructor 2: "+hashCode());
	}

	@Override
	public void onDraw(Canvas canvas){
		super.onDraw(canvas);
		drawIndicatorCircle(canvas);
		drawIndicator(canvas);
	}

/*
	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
		setMeasuredDimension(heightMeasureSpec / 4, heightMeasureSpec);
	}
*/
	public void setState(int turnoutState, boolean animate){
        //Log.d(TAG, "setState(): "+turnoutState+", previous state: "+currentState+", hash: "+hashCode());
		if(turnoutState!=currentState){
            //Log.d(TAG, "State is different");
			currentState = turnoutState;
			int finalIndicatorPosition = getIndicatorPositionFromState(currentState);
			if(animate){
				animator = new ValueAnimator();
				animator.setDuration(ANIMATION_DURATION_MS);
				animator.setIntValues(indicatorAngle, finalIndicatorPosition);
				animator.addUpdateListener(this);
//				animator.setInterpolator(new BounceInterpolator());
				animator.start();
			}
			else{
				indicatorAngle = finalIndicatorPosition;
				invalidate();
			}
		}
	}

	public int getCurrentState(){
		return currentState;
	}

	private void drawIndicator(Canvas canvas) {
		int startX = left;
		int startY = midPointY;
		int lineLength = useableWidth;
		paintIndicator.setColor(getColourFromState(currentState));
		double angleRadians = Math.toRadians(indicatorAngle);
		int endX = (int) (startX + (lineLength * Math.sin(angleRadians)));
		int endY = (int) (startY + (lineLength * Math.cos(angleRadians)));
		
		canvas.drawLine(startX, startY, endX, endY, paintIndicator);
		
		//canvas.drawRect(left, indicatorAngle - halfIndicatorHeight, right, indicatorAngle + halfIndicatorHeight, paintIndicator);
	}
	
	private void drawIndicatorCircle(Canvas canvas) {
		canvas.drawCircle(left, midPointY, lineWidthPx, paintIndicator);
	}

	@Override
	public void onInitialise() {
		super.onInitialise();

        //Log.d(TAG, "Setting state to unknown");
		currentState = Turnout.STATE_UNKNOWN;

//		indicatorHeight = useableHeight / 3;
		
		indicatorAngle = indicatorAngleUnknown;

		lineWidthPx = Utils.convertDpToPixels(LINE_WIDTH_DP, getContext());
		
		Resources resources = getResources();
		if(!isInEditMode()){
			colourUnknown = resources.getColor(R.color._CCCCCC);
			colourKnown = resources.getColor(android.R.color.black);
		}
		else{
			colourUnknown = 0xFFCCCCCC;
			colourKnown = 0xFF000000;
		}
		//colourThrown = resources.getColor(R.color._669900);
		//colourClosed = resources.getColor(R.color._CC0000);

		paintIndicator = new Paint();
		paintIndicator.setStyle(Style.FILL_AND_STROKE);
		paintIndicator.setStrokeWidth(lineWidthPx);
		paintIndicator.setAntiAlias(true);
	}

	private int getIndicatorPositionFromState(int turnoutState){
		int indicatorAngle;
		switch(turnoutState){
		case Turnout.STATE_CLOSED:
			indicatorAngle = indicatorAngleClosed;
			break;
		case Turnout.STATE_THROWN:
			indicatorAngle = indicatorAngleThrown;
			break;
		default:
		case Turnout.STATE_UNKNOWN:
			indicatorAngle = indicatorAngleUnknown;
			break;
		}
		return indicatorAngle;
	}

	private int getColourFromState(int turnoutState){
		int colour;
		switch(turnoutState){
		case Turnout.STATE_CLOSED:
		case Turnout.STATE_THROWN:
			colour = colourKnown;
			break;
		default:
		case Turnout.STATE_UNKNOWN:
			colour = colourUnknown;
			break;
		}
		return colour;
	}
	
	@Override

	protected int getBackgroundColourResId(){
		return android.R.color.transparent;
	}

	@Override
	public void onAnimationUpdate(ValueAnimator animator) {
		indicatorAngle = (Integer) animator.getAnimatedValue();
		invalidate();
	}
}