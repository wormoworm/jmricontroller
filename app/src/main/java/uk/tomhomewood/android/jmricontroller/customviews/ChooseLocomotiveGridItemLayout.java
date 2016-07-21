package uk.tomhomewood.android.jmricontroller.customviews;

import android.content.Context;
import android.util.AttributeSet;

public class ChooseLocomotiveGridItemLayout extends FixedAspectRatioRelativeLayout {

	public ChooseLocomotiveGridItemLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected float getAspectRatio() {
		return 3f;
	}
}