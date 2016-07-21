package uk.tomhomewood.android.jmricontroller.customviews;

import uk.tomhomewood.android.jmricontroller.R;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;

public class SmartView extends View {
//	private final String TAG = "SmartView";

	protected int top, right, bottom, left, midPointX, midPointY;
	protected int useableHeight, useableWidth;

	protected boolean initialised, dimensionsInitialised;
	
	protected Paint paintBackground;
	
	protected Resources resources;

	public SmartView(Context context) {
		super(context);
		initialised = false;
	}

	public SmartView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialised = false;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		if(!dimensionsInitialised){
			onInitialiseDimensions();
		}
	}
	
	protected void onInitialiseDimensions() {
		dimensionsInitialised = true;
	}

	@Override
	public void onDraw(Canvas canvas){
		if(!initialised){
			onInitialise();
		}
		drawBackground(canvas);
	}

	public void onInitialise(){
		int width = getMeasuredWidth();
		int height = getMeasuredHeight();
		top = getPaddingTop();
		right = width - getPaddingRight();
		bottom = height - getPaddingBottom();
		left = getPaddingLeft();

		useableWidth = right - left;
		useableHeight = bottom - top;

		midPointX = (left + right) / 2;
		midPointY = (top + bottom) / 2;
		
		resources = getResources();
		
		paintBackground = new Paint();
		paintBackground.setStyle(Style.FILL);
		paintBackground.setAntiAlias(true);
		int backgroundColour;
		if(!isInEditMode()){
			backgroundColour = resources.getColor(getBackgroundColourResId());
		}
		else{
			backgroundColour = 0xFFFFFFFF;
		}
		paintBackground.setColor(backgroundColour);

		initialised = true;
	}

	private void drawBackground(Canvas canvas) {
		canvas.drawRect(left, top, right, bottom, paintBackground);
	}

	protected int getBackgroundColourResId() {
		return android.R.color.white;
	}

	protected boolean touchInsideUseableX(int touchX) {
		return touchX>left && touchX<right;
	}

	protected boolean touchInsideUseableY(int touchY) {
		return touchY>top && touchY<bottom;
	}

	protected boolean touchInsideUseableXY(int touchX, int touchY) {
		return touchInsideUseableX(touchX) && touchInsideUseableY(touchY);
	}
}