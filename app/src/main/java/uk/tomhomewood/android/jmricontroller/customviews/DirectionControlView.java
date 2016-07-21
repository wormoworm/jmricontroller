package uk.tomhomewood.android.jmricontroller.customviews;

import uk.tomhomewood.android.jmricontroller.R;
import uk.tomhomewood.android.jmricontroller.Utils;
import uk.tomhomewood.android.jmricontroller.network.WiThrottleCommandClientThrottle.Direction;
import uk.tomhomewood.smart.common.Typefaces;
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

public class DirectionControlView extends SmartView implements AnimatorListener, AnimatorUpdateListener {
	private final String TAG = "DirectionControlView";

	private final int LINE_WIDTH_DP = 4;
	private final long ANIMATION_DURATION_MS = 300;

	private Context context;
	private DirectionControlListener directionControlListener;
	private Direction currentDirection;
	
	private boolean setSliderValueFromCurrentDirection;

	private Paint paintLines, paintSlider, paintSliderText, paintBackgroundText;

	private int lineWidthPx, sliderWidthPx;
	private int halfSliderWidth;
	//	private int sliderUseableHeight;
	private int sliderPosition, sliderPositionReverse, sliderPositionStopped, sliderPositionForward;
	private int sliderTextSizePx, sliderTextYCompensationPx;

	private ValueAnimator animator;

	public DirectionControlView(Context context) {
		super(context);
		this.context = context;
		currentDirection = Direction.FORWARD;
	}

	public DirectionControlView(Context context, AttributeSet attributes) {
		super(context, attributes);
		this.context = context;  
		currentDirection = Direction.FORWARD;
	}

	public void setDirection(Direction direction, boolean animate){
		currentDirection = direction;
		if(!initialised){
			Log.w(TAG, "Set direction before initialised");
			setSliderValueFromCurrentDirection = true;
			invalidate();
		}
		int finalsliderPosition = getSliderPositionFromDirection(direction);
		setSliderPosition(finalsliderPosition, animate);
	}

	private void setSliderPosition(int position, boolean animate) {
		if(animate){
			animator = new ValueAnimator();
			animator.setDuration(ANIMATION_DURATION_MS);
			animator.setIntValues(sliderPosition, position);
			animator.addUpdateListener(this);
			animator.start();
		}
		else{
			sliderPosition = position;
			invalidate();
		}
	}

	public void setStopped(boolean animate) {
		Log.d(TAG, "Set stopped");
		setSliderPosition(sliderPositionStopped, animate);
	}

	public void setDirectionControlListener(DirectionControlListener directionControlListener){
		this.directionControlListener = directionControlListener;
	}

	@Override
	public void onDraw(Canvas canvas){
		super.onDraw(canvas);
		drawBackgroundText(canvas);
		drawSlider(canvas);
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event){
		getParent().requestDisallowInterceptTouchEvent(true);
		int eventX = Math.round(event.getX());
		int eventY = Math.round(event.getY());
		int eventAction = event.getAction();
		//Log.d(TAG, "Event: "+eventAction);
		switch (eventAction) {
		case MotionEvent.ACTION_DOWN:
			if(touchInsideUseableXY(eventX, eventY)){
				if(touchInsideSliderX(eventX)){
					sliderPosition = clipSliderPosition(eventX);
					invalidate();
				}
			}
			break;
		case MotionEvent.ACTION_MOVE:
			//updateDirectionAndSlider(eventX);
			//sliderPosition = eventX;
			if(touchInsideSliderX(eventX)){
				sliderPosition = clipSliderPosition(eventX);
				invalidate();
			}
			break;
		case MotionEvent.ACTION_UP:
//			if(touchInsideUseableX(eventX)){
				updateSliderAndDirection(eventX);
//			}
		default:
			break;
		}
		return true;
	}

	@Override
	protected int getBackgroundColourResId() {
		return R.color.driving_controls_grid;
	}

	private boolean touchInsideSliderX(int touchX) {
		int sliderLeft = sliderPosition - halfSliderWidth;
		int sliderRight = sliderPosition + halfSliderWidth;
		return touchX > sliderLeft && touchX < sliderRight;
	}

	@Override
	protected void onInitialiseDimensions() {
		super.onInitialiseDimensions();
	}

	@Override
	public void onInitialise() {
		super.onInitialise();

		sliderWidthPx = useableWidth / 3;
		halfSliderWidth = sliderWidthPx / 2;
		sliderPositionReverse = left + halfSliderWidth;
		sliderPositionStopped = (left + right) / 2;
		sliderPositionForward = right - halfSliderWidth;
		Log.d(TAG, "Set SPF: "+sliderPositionForward);

		sliderPosition = sliderPositionStopped;

		lineWidthPx = Utils.convertDpToPixels(LINE_WIDTH_DP, context);
		if(!isInEditMode()){
			sliderTextSizePx = getResources().getDimensionPixelSize(R.dimen.text_size_large);
		}
		else{
			sliderTextSizePx = 52;
		}
		//		sliderTextPaddingPx = Utils.convertDpToPixels(SLIDER_TEXT_PADDING_DP, context);
		//		sliderHeightPx = sliderTextSizePx + (sliderTextPaddingPx * 2);

		sliderTextYCompensationPx = Math.round((float) sliderTextSizePx * 0.14f);

		//		halfSliderHeight = sliderHeightPx / 2;
		//		maxSliderY = top + halfSliderHeight + lineWidthPx;
		//		minSliderY = bottom - halfSliderHeight - lineWidthPx;
		//		sliderUseableHeight = minSliderY - maxSliderY;
		paintLines = new Paint();
		paintLines.setStyle(Style.STROKE);
		paintLines.setStrokeWidth(lineWidthPx);
		paintLines.setAntiAlias(true);
		paintLines.setColor(0xFF666666);

		paintSlider = new Paint();
		paintSlider.setStyle(Style.FILL);
		paintSlider.setAntiAlias(true);

		paintSliderText = new Paint();
		paintSliderText.setStyle(Style.FILL);
		paintSliderText.setAntiAlias(true);
		paintSliderText.setColor(0xFFFFFFFF);
        paintSliderText.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		paintSliderText.setTextSize(sliderTextSizePx);
		paintSliderText.setTextAlign(Align.CENTER);
		
		paintBackgroundText = new Paint();
		paintBackgroundText.setStyle(Style.FILL);
		paintBackgroundText.setAntiAlias(true);
		paintBackgroundText.setColor(0xFFFFFFFF);
		paintBackgroundText.setTextSize(sliderTextSizePx);
		paintBackgroundText.setTextAlign(Align.CENTER);
        paintBackgroundText.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));

		paintSlider.setColor(resources.getColor(R.color.driving_controls_primary));
	}

	private void drawBackgroundText(Canvas canvas) {
		drawBackgroundText(canvas, sliderPositionReverse);
		drawBackgroundText(canvas, sliderPositionStopped);
		drawBackgroundText(canvas, sliderPositionForward);
	}

	private void drawBackgroundText(Canvas canvas, int position) {
		canvas.drawText(getSliderText(position), position, midPointY + (sliderTextSizePx / 2) - sliderTextYCompensationPx, paintBackgroundText);
	}

	private void drawSlider(Canvas canvas){
		if(setSliderValueFromCurrentDirection){
			sliderPosition = getSliderPositionFromDirection(currentDirection);
			setSliderValueFromCurrentDirection = false;
		}
		canvas.drawRect(sliderPosition - halfSliderWidth, top, sliderPosition + halfSliderWidth, bottom, paintSlider);
		canvas.drawText(getSliderText(sliderPosition), sliderPosition, midPointY + (sliderTextSizePx / 2) - sliderTextYCompensationPx , paintSliderText);
	}

	private String getSliderText(int xPosition) {
		String text;
		int position = (xPosition - left) / sliderWidthPx;
		switch(position){
		case 0:
			text = "R";
			break;
		default:
		case 1:
			text = "S";
			break;
		case 2:
			text = "F";
			break;
		}
		return text;
	}

	private int clipSliderPosition(int rawPosition) {
		int newPosition;
		if(rawPosition<sliderPositionReverse){
			newPosition = sliderPositionReverse;
		}
		else if(rawPosition>sliderPositionForward){
			newPosition = sliderPositionForward;
		}
		else{
			newPosition = rawPosition;
		}
		return newPosition;
	}


	private void updateSliderAndDirection(int xPosition) {
		//First, save the new slider position
		int newPosition = getSliderPositionFromTouchPosition(xPosition);
		setSliderPosition(newPosition, true);
		//Next, calculate the speed from the position
		//Finally, send a speed changed event to the event listener
		if(directionControlListener!=null){
			if(newPosition==sliderPositionReverse){
				currentDirection = Direction.REVERSE;
				directionControlListener.directionChanged(currentDirection);
			}
			else if(newPosition==sliderPositionStopped){
				directionControlListener.stopped();
			}
			else if(newPosition==sliderPositionForward){
				currentDirection = Direction.FORWARD;
				directionControlListener.directionChanged(currentDirection);
			}
		}
	}

	private int getSliderPositionFromTouchPosition(int xPosition) {
		int sliderPosition;
		int clippedPosition = clipSliderPosition(xPosition);
		int position = (clippedPosition - left) / sliderWidthPx;
		switch(position){
		case 0:
			sliderPosition = sliderPositionReverse;
			break;
		default:
		case 1:
			sliderPosition = sliderPositionStopped;
			break;
		case 2:
			sliderPosition = sliderPositionForward;
			break;
		}
		return sliderPosition;
	}

	private int getSliderPositionFromDirection(Direction direction) {
		Log.d(TAG, "Get position: "+direction);
		int sliderPosition = sliderPositionForward;
		if(direction==Direction.FORWARD){
			Log.d(TAG, "Forward");
			sliderPosition = sliderPositionForward;
		}
		if(direction==Direction.REVERSE){
			Log.d(TAG, "Reverse");
			sliderPosition = sliderPositionReverse;
		}
		Log.d(TAG, "Position: "+sliderPosition);
		return sliderPosition;
	}

	@Override
	public void onAnimationUpdate(ValueAnimator animator) {
		sliderPosition = (Integer) animator.getAnimatedValue();
		invalidate();
	}

	@Override
	public void onAnimationCancel(Animator animator) {
	}

	@Override
	public void onAnimationEnd(Animator animator) {
	}

	@Override
	public void onAnimationRepeat(Animator animator) {
	}

	@Override
	public void onAnimationStart(Animator animator) {
	}

	public interface DirectionControlListener{

		public void directionChanged(Direction direction);

		public void stopped();
	}
}