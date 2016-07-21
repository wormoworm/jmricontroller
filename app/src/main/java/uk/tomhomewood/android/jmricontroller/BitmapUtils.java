package uk.tomhomewood.android.jmricontroller;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

public class BitmapUtils {
	private static final String TAG = "BitmapUtils"; 
	
	public static Bitmap getMapLabel(Context context, String text, Integer textSizeDp, Boolean textBold, Integer paddingDp, Integer textColour, Integer backgroundColour){
		if(textSizeDp==null){
			textSizeDp = 20;
		}
		if(paddingDp==null){
			paddingDp = 10;
		}
		if(textBold==null){
			textBold = false;
		}
		if(textColour==null){
			textColour = 0xFF000000;
		}
		if(backgroundColour==null){
			backgroundColour = 0xFFFF8800;
		}
		int textSizePx = Utils.convertDpToPixels(textSizeDp, context);
		int paddingPx = Utils.convertDpToPixels(paddingDp, context);
		
		Paint paintBackground = new Paint();
		paintBackground.setStyle(Paint.Style.FILL);
		paintBackground.setColor(backgroundColour);
		
		Paint paintText = new Paint();
		paintText.setTextSize(textSizePx);
		paintText.setTextAlign(Paint.Align.CENTER);
		paintText.setColor(textColour);
		paintText.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		paintText.setAntiAlias(true);
		
		Paint paintBackground2 = new Paint();
		paintBackground2.setStyle(Paint.Style.FILL);
		paintBackground2.setColor(textColour);
		
		Rect textBounds = new Rect();
		paintText.getTextBounds(text, 0, text.length(), textBounds);

		int imageWidth = textBounds.width() + (paddingPx * 2);
		int imageHeight = textSizePx + (paddingPx * 2);
		
		//Log.d();
		
		Bitmap image = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(image);
		
		canvas.drawRect(0, 0, imageWidth, imageHeight, paintBackground);
		Log.d(TAG, "Bounds: "+textBounds.toShortString());
		//canvas.drawRect(textBounds.left, textBounds.top, textBounds.right, textBounds.bottom, paintBackground2);
		
		canvas.drawText(text, imageWidth / 2, textBounds.height() - textBounds.bottom + paddingPx, paintText);
		canvas.save();
		
		return image;
	}
	
	public static BitmapFactory.Options getImageInformation(String imagePath){
		BitmapFactory.Options options = new BitmapFactory.Options();                //This options object will be passed to the decode() function
		options.inJustDecodeBounds = true;                                          //Forces the decode() function to only return basic information about the image, not the image's contents
		BitmapFactory.decodeFile(imagePath, options);
		return options;
	}
	
	public static int calculateSubSamplingFactor(BitmapFactory.Options options, int reqWidth, int reqHeight, boolean shrinkImage) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
    
        if (height > reqHeight || width > reqWidth) {
            // Calculate ratios of height and width to requested height and width
            final float heightRatio = ((float) height / (float) reqHeight);
            final float widthRatio = ((float) width / (float) reqWidth);
    
           if(shrinkImage){                //If this parameter is true, take the smallest ratio. This will mean that both dimensions are under the limits provided
                    inSampleSize = (int) Math.ceil(heightRatio > widthRatio ? heightRatio : widthRatio);
            }
            else{                                //Otherwise, take the largest ratio, which will mean that both the dimensions are over the limits
                    inSampleSize = (int) Math.floor(heightRatio < widthRatio ? heightRatio : widthRatio);
            }
        }
        return inSampleSize;
    }

	public static class LoadBitmapAsync extends AsyncTask<Void, Void, Bitmap>{
		//private final String TAG = "LoadBitmapAsync";
		private String imagePath;
		private String imagePathFull;
		private final WeakReference<ImageView> imageViewReference;                //The ImageView is referenced from this to prevent it being garbage collected
		private Integer width = null;
		private Integer height = null;
		private boolean fade = false;
		private LruCache<String, Bitmap> cache = null;

		public LoadBitmapAsync(String newImagePath, ImageView imageView, Integer desiredWidth, Integer desiredHeight, boolean fadeIn, LruCache<String, Bitmap> newCache){
			imagePath = newImagePath;
			imagePathFull = imagePath+desiredWidth+desiredHeight;                        //For the caching
			imageViewReference = new WeakReference<ImageView>(imageView);
			width = desiredWidth;
			height = desiredHeight;
			fade = fadeIn;
			cache = newCache;
		}
		@Override
		protected Bitmap doInBackground(Void...params){
			//The image we load will be downsampled when loading to reduce the memory footprint. To do this, we need to know the factor by which to downsample
			BitmapFactory.Options options = getImageInformation(imagePath);
			float originalWidth = options.outWidth;                                //Original dimensions of the image
			float originalHeight = options.outHeight;

			//Load the Bitmap from the file. This is normally the part that takes the most time
			Bitmap outputImage = null;

			//Calculate the size of the desired image, as some information may be missing (maybe only the width or height was provided). Once we know this information, we can calculate the scaling factor
			if(width==null && height==null){                //No scaling, simply return the image as it was loaded from disk
				width = (int) originalWidth;
				height = (int) originalHeight;
			}
			else if(width!=null){
				height = (int) (originalHeight / originalWidth * width);
			}
			else if(height!=null){
				width = (int) (originalWidth / originalHeight * height);
			}
			int scalingFactor = calculateSubSamplingFactor(options, width, height, false);
			options.inSampleSize = scalingFactor;
			options.inJustDecodeBounds = false;                //We want the Bitmap's pixels this time
			outputImage = BitmapFactory.decodeFile(imagePath, options);                                //This will load the bitmap and subsample with the value we provided, to minimise memory usage
			//Log.d(TAG, "width: "+outputImage.getWidth()+", height: "+outputImage.getHeight()+", Owidth: "+originalWidth+", Oheight: "+originalHeight);
			if(cache!=null && outputImage!=null){                //If a cache reference was provided, store the loaded image in the cache
				cache.put(imagePathFull, outputImage);
			}
			//Log.d(TAG, "IMAGE BYTES: "+outputImage.getByteCount());
			return outputImage;
		}

		protected void onPostExecute(Bitmap imageBitmap){
			if(imageViewReference!=null && imageBitmap!=null) {
				final ImageView imageView = imageViewReference.get();
				if (imageView != null) {
					imageView.setImageBitmap(imageBitmap);
					if(fade){
						imageView.setVisibility(View.INVISIBLE);
						Animation fadeInAnimation = AnimationUtils.loadAnimation(imageView.getContext(), R.anim.fade_in);
						imageView.startAnimation(fadeInAnimation);
						fadeInAnimation.setAnimationListener(new AnimationListener() {
							@Override
							public void onAnimationEnd(Animation animation) {
								imageView.setVisibility(View.VISIBLE);
							}
							@Override
							public void onAnimationRepeat(Animation animation) {}
							@Override
							public void onAnimationStart(Animation animation) {}
						});
					}
				}
			}
		}
	}
}
