package uk.tomhomewood.android.jmricontroller.customviews;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class SquareImageView extends ImageView{

	public SquareImageView(Context context) {
		super(context);
	}

	public SquareImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec){

		int widthSize = MeasureSpec.getSize(widthMeasureSpec);      
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);

		// Restrict the aspect ratio to 1:1, fitting within original specified dimensions
		int chosenDimension = Math.min(widthSize, heightSize);
		widthMeasureSpec = MeasureSpec.makeMeasureSpec(chosenDimension, MeasureSpec.AT_MOST);
		heightMeasureSpec = MeasureSpec.makeMeasureSpec(chosenDimension, MeasureSpec.AT_MOST);

		getLayoutParams().height = chosenDimension;
		getLayoutParams().width = chosenDimension;
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
}