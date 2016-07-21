package uk.tomhomewood.android.jmricontroller.customviews;

import uk.tomhomewood.android.jmricontroller.R;
import uk.tomhomewood.android.jmricontroller.Utils;
import uk.tomhomewood.smart.common.Typefaces;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

public class SpeedControlView extends SmartView {
	private final String TAG = "SpeedControlView";
	
//	private final int TOUCH_STATE_WAITING_FOR_DOWN = 1;
//	private final int TOUCH_STATE_DOWN = 2;
//	private final int TOUCH_STATE_UP = 3;
//	private final int TOUCH_STATE_SCROLL = 4;

	private static final float CHANGE_RATIO_THRESHOLD = 2.0f;

	private final int BUFFER_FRAMES = 5;

	private final int LINE_WIDTH_DP = 4;
	private final int SLIDER_TEXT_PADDING_DP = 4;

	private float downTouchEventX, downTouchEventY;
	private float lastMoveTouchEventX, lastMoveTouchEventY;
	
	private boolean touchDown = false;
	
	private Context context;
	private SpeedControlListener speedControlListener; 
	private int currentSpeed;
	private int maxSpeed;

	private boolean enabled;

	private Paint paintLines, paintSlider, paintSliderText;

	private int lineWidthPx, sliderHeightPx;
	private float halfSliderHeight;
	private int maxSliderY, minSliderY;
	private int sliderUseableHeight;
	private int sliderPosition;
	private int sliderTextSizePx, sliderTextPaddingPx, sliderTextYCompensationPx;

	public SpeedControlView(Context context) {
		super(context);
		this.context = context;
		currentSpeed = 0;
		maxSpeed = 100;
		enabled = true;
	}

	public SpeedControlView(Context context, AttributeSet attributes) {
		super(context, attributes);
		this.context = context;
		currentSpeed = 0;
		maxSpeed = 100;
		enabled = true;
		touchDown = false;
	}

	public void setSliderEnabled(boolean enabled){
		this.enabled = enabled;
	}

	public void setMaxSpeed(int maxSpeed){
		this.maxSpeed = maxSpeed;
		initialised = false;
		invalidate();
	}

	public void setSpeed(int speed){
		currentSpeed = speed;
		sliderPosition = calculateSliderPositionFromSpeed(currentSpeed);
		invalidate();
	}

	public void setSpeedControlListener(SpeedControlListener speedControlListener){
		this.speedControlListener = speedControlListener;
	}

	@Override
	public void onDraw(Canvas canvas){
		super.onDraw(canvas);
		drawLines(canvas);
		drawSlider(canvas);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event){
		if(enabled){
	//		Log.d(TAG, "Event: "+event.getAction());
			int eventAction = event.getAction();
	//		getParent().requestDisallowInterceptTouchEvent(touchDown);
			switch (eventAction) {
			case MotionEvent.ACTION_DOWN:
				if(!touchDown){
					downTouchEventX = event.getX();
					downTouchEventY = event.getY();
					postDelayedDownMessage();
					return true;
				}
			case MotionEvent.ACTION_MOVE:
	//			Log.d(TAG, "Touch down: "+touchDown);
				if(touchDown){
					updateSpeedAndSlider((int) event.getY());
					break;
				}
				else{
					lastMoveTouchEventX = event.getX();
					lastMoveTouchEventY = event.getY();
	//				return false;
					break;
				}
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP:
				if(touchDown){
					upEvent();
				}
				removeDelayedDownMessage();
	//			return super.onTouchEvent(event);
				break;
			default:
				break;
			}
			if(touchDown){
				return super.onTouchEvent(event);
			}
			else{
				return false;
			}
		}
		else{
			return super.onTouchEvent(event);
		}
	}
	
	private void postDelayedDownMessage() {
		postDelayed(delayedDownMessage, (BUFFER_FRAMES - 1) * 18);
	}
	
	private void removeDelayedDownMessage() {
		removeCallbacks(delayedDownMessage);
	}
	
	private Runnable delayedDownMessage = new Runnable() {
		@Override
		public void run() {
			if(changeRatioExceeded()){		//Scrolling
				upEvent();
				getParent().requestDisallowInterceptTouchEvent(false);
			}
			else{							//Sliding
				downEvent();
				getParent().requestDisallowInterceptTouchEvent(true);
				updateSpeedAndSlider((int) downTouchEventY);
			}
		}
	};
	
	private void downEvent() {
		touchDown = true;
	}
	
	private boolean changeRatioExceeded() {
//		Log.d(TAG, "Change ratio: "+getChangeRatio());
		
		return getChangeRatio() > CHANGE_RATIO_THRESHOLD;
	}
	
	private float getChangeRatio(){
		float dX = Math.abs(lastMoveTouchEventX - downTouchEventX);
		float dY = Math.abs(lastMoveTouchEventY - downTouchEventY);
		if(dY==0){
			dY = 1f;
		}
//		Log.d(TAG, "dX: "+dX+", dY: "+dY);
		return dX / dY;
	}

	private void upEvent() {
		touchDown = false;
	}

	@Override
	protected int getBackgroundColourResId() {
		return R.color._FFFFFF;
	}

	@Override
	public void onInitialise() {
		super.onInitialise();

		lineWidthPx = Utils.convertDpToPixels(LINE_WIDTH_DP, context);
		if(!isInEditMode()){
			sliderTextSizePx = getResources().getDimensionPixelSize(R.dimen.text_size_large);
		}
		else{
			sliderTextSizePx = 52;
		}
		sliderTextPaddingPx = Utils.convertDpToPixels(SLIDER_TEXT_PADDING_DP, context);
//		sliderHeightPx = sliderTextSizePx + (sliderTextPaddingPx * 2);
		sliderHeightPx = getResources().getDimensionPixelSize(R.dimen.driving_touchable_component_height);

		sliderTextYCompensationPx = Math.round((float) sliderTextSizePx * 0.14f);
		halfSliderHeight = sliderHeightPx / 2;
		maxSliderY = (int) Math.floor(top + halfSliderHeight + lineWidthPx - 0.5f);
		minSliderY = (int) Math.ceil(bottom - halfSliderHeight - lineWidthPx + 0.5f);
		sliderUseableHeight = minSliderY - maxSliderY;

		sliderPosition = calculateSliderPositionFromSpeed(currentSpeed);

		paintLines = new Paint();
		paintLines.setStyle(Style.STROKE);
		paintLines.setStrokeWidth(lineWidthPx);
		paintLines.setColor(resources.getColor(R.color.driving_controls_grid));

		paintSlider = new Paint();
		paintSlider.setStyle(Style.FILL);
		paintSlider.setColor(resources.getColor(R.color.driving_controls_primary));

		paintSliderText = new Paint();
		paintSliderText.setStyle(Style.FILL);
		paintSliderText.setColor(0xFFFFFFFF);
		paintSliderText.setAntiAlias(true);
		paintSliderText.setTextSize(sliderTextSizePx);
		paintSliderText.setTextAlign(Align.CENTER);
		if(!isInEditMode()){
			paintSliderText.setTypeface(Typefaces.getFont(context, "lato_black"));
		}
	}

	private void drawLines(Canvas canvas) {
		int topLineY = top + (lineWidthPx / 2);
		int bottomLineY = bottom - (lineWidthPx / 2);
		int middleX = (left + right) / 2;
		canvas.drawLine(left, topLineY, right, topLineY, paintLines);
		canvas.drawLine(left, bottomLineY, right, bottomLineY, paintLines);
		canvas.drawLine(middleX, top, middleX, bottom, paintLines);
	}

	private void drawSlider(Canvas canvas){
		canvas.drawRect(left, sliderPosition - halfSliderHeight, right, sliderPosition + halfSliderHeight, paintSlider);
		canvas.drawText(""+currentSpeed, (left + right) / 2, sliderPosition - halfSliderHeight + sliderTextSizePx + sliderTextPaddingPx + sliderTextYCompensationPx, paintSliderText);
	}

	private void updateSpeedAndSlider(int yPosition) {
		//First, save the new slider position
		sliderPosition = checkSliderPosition(yPosition);
		//Next, calculate the speed from the position
		currentSpeed = calculateSpeedFromSliderPosition(sliderPosition);
		//Trigger a redraw
		invalidate();
		//Finally, send a speed changed event to the event listener
		if(speedControlListener!=null){
			speedControlListener.speedChanged(currentSpeed);
		}
	}

	private int checkSliderPosition(int yPosition) {
		int checkedPosition;
		if(yPosition < maxSliderY){				//True if the touch is above the maximum Y position of the vertical centerline of the slider
			checkedPosition = maxSliderY;		//Clip to the top
		}
		else if(yPosition > minSliderY){		//True if the touch is below the minimum Y position of the vertical centerline of the slider
			checkedPosition = minSliderY;		//Clip to the bottom
		}
		else{									//The touch is somewhere between the top and bottom, and not too close to the extents
			checkedPosition = yPosition;
		}
		return checkedPosition;
	}

	private int calculateSpeedFromSliderPosition(int sliderPosition) {
		float pxPerSpeedUnit = (float) (sliderUseableHeight)  / maxSpeed;
		int sliderOffset = sliderUseableHeight - (sliderPosition - maxSliderY);
		int speed = Math.round((float) sliderOffset / pxPerSpeedUnit);
		return speed;
	}

	private int calculateSliderPositionFromSpeed(int currentSpeed) {
		float speedUnitPerPx = (float) sliderUseableHeight  / maxSpeed;
		int speedOffset = maxSpeed - currentSpeed;
		int sliderPosition = Math.round(maxSliderY + (speedOffset * speedUnitPerPx));
		return sliderPosition;
	}

	public interface SpeedControlListener{

		public void speedChanged(int speed);
	}
}