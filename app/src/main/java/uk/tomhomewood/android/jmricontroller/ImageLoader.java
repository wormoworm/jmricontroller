package uk.tomhomewood.android.jmricontroller;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.view.animation.Animation;
import android.widget.ImageView;

import java.util.HashMap;

public class ImageLoader {
    private final String TAG = "ImageLoader";

    private BitmapCache bitmapCache;

    private HashMap<String, ImageView> imagesBeingLoaded;

    public ImageLoader(){
        bitmapCache = BitmapCache.getCache();
        imagesBeingLoaded = new HashMap<>();
    }

    public void loadImage(String imagePath, Integer desiredWidth, Integer desiredHeight, final ImageView imageView, final Animation loadedAnimation){
        final String cacheKey = generateBitmapCacheKey(imagePath, imageView);
        if(bitmapCache.hasBitmap(cacheKey)){                            //Bitmap already exists
            imageView.setImageBitmap(bitmapCache.getBitmap(cacheKey));
        }
        else{           //Bitmap does not exist, so we need to load it
            if(!isLoadingImage(imagePath, imageView)) {
                imagesBeingLoaded.put(cacheKey, imageView);
                if (desiredWidth == null) {
                    desiredWidth = imageView.getMeasuredWidth();
                }
                if (desiredHeight == null) {
                    desiredHeight = imageView.getMeasuredHeight();
                }
                new AsyncBitmapLoader(imagePath, desiredWidth, desiredHeight, new AsyncBitmapLoader.BitmapLoaderListener() {
                    @Override
                    public void bitmapLoaded(String imagePath, Bitmap bitmap) {
                        if (loadedAnimation != null) {
                            imageView.startAnimation(loadedAnimation);
                        }
                        imageView.setImageBitmap(bitmap);
                        bitmapCache.storeBitmap(cacheKey, bitmap);
                        imagesBeingLoaded.remove(cacheKey);
                    }

                    @Override
                    public void loadFailed(String imagePath) {
                        Log.e(TAG, "Failed to load bitmap from :" + imagePath);
                        imagesBeingLoaded.remove(cacheKey);
                    }
                }).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
            }
        }
    }

    public void loadImage(String imagePath, ImageView imageView){
        loadImage(imagePath, null, null, imageView, null);
    }

    public void loadImage(String imagePath, ImageView imageView, Animation loadedAnimation){
        loadImage(imagePath, null, null, imageView, loadedAnimation);
    }

    public boolean isLoadingImage(String imagePath, ImageView imageView){
        return imagesBeingLoaded.containsKey(generateBitmapCacheKey(imagePath, imageView));
    }

    private String generateBitmapCacheKey(String imagePath, ImageView imageView) {
        String key = imagePath + imageView.getMeasuredWidth() + imageView.getMeasuredHeight();
        return key;
    }
}