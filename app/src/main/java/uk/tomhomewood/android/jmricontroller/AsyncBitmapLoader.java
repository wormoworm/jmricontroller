package uk.tomhomewood.android.jmricontroller;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

public class AsyncBitmapLoader extends AsyncTask<Void, Void, Bitmap>{
    private final String TAG = "AsyncBitmapLoader";

    private String imagePath;
    private BitmapLoaderListener listener;

    private Integer width, height;

    public AsyncBitmapLoader(String imagePath, Integer desiredWidth, Integer desiredHeight, BitmapLoaderListener bitmapLoaderListener) {
        Log.d(TAG, "Loading image from disk, width: "+desiredWidth+", height: "+desiredHeight);
        width = desiredWidth;
        height = desiredHeight;
        if(bitmapLoaderListener==null){
            throw new IllegalArgumentException("Listener cannot be null");
        }
        this.imagePath = imagePath;
        listener = bitmapLoaderListener;
    }

    @Override
    protected Bitmap doInBackground(Void... params) {
        long startTime = System.currentTimeMillis();

        //The image we load will be downsampled when loading to reduce the memory footprint. To do this, we need to know the factor by which to downsample
        BitmapFactory.Options options = getImageInformation(imagePath);
        float originalWidth = options.outWidth;                                //Original dimensions of the image
        float originalHeight = options.outHeight;

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
        Bitmap outputImage = BitmapFactory.decodeFile(imagePath, options);                                //This will load the bitmap and subsample with the value we provided, to minimise memory usage

        long endTime = System.currentTimeMillis();

        Log.d(TAG, "Loaded bitmap ("+outputImage.getByteCount()+" bytes) in "+(endTime - startTime)+"ms");

        return outputImage;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if(bitmap!=null){
            listener.bitmapLoaded(imagePath, bitmap);
        }
        else{
            listener.loadFailed(imagePath);
        }
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

    public interface BitmapLoaderListener{

        public void bitmapLoaded(String imagePath, Bitmap bitmap);

        public void loadFailed(String imagePath);
    }
}