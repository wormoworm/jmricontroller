package uk.tomhomewood.android.jmricontroller.customviews;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import uk.tomhomewood.android.jmricontroller.R;

public class FixedAspectRatioRelativeLayout extends RelativeLayout{

    private final float ASPECT_RATIO_DEFAULT = 1f;
    private float aspectRatio = ASPECT_RATIO_DEFAULT;

    public FixedAspectRatioRelativeLayout(Context context) {
		super(context);
	}
	
	public FixedAspectRatioRelativeLayout(Context context, AttributeSet attributes) {
		super(context, attributes);
        TypedArray properties = context.obtainStyledAttributes(attributes, R.styleable.FixedAspectRatioRelativeLayout);
        aspectRatio = properties.getFloat(R.styleable.FixedAspectRatioRelativeLayout_aspect_ratio, ASPECT_RATIO_DEFAULT);
        properties.recycle();
	}

	@Override
	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec){
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);      
		int heightSize = (int) (widthSize / getAspectRatio());

		heightMeasureSpec = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.AT_MOST);

//		getLayoutParams().height = widthSize;
//		getLayoutParams().width = heightSize;
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);    
	}

	protected float getAspectRatio(){
        return aspectRatio;
    }
}