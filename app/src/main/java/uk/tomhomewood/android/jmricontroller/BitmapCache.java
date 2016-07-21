package uk.tomhomewood.android.jmricontroller;

import android.graphics.Bitmap;
import android.util.Log;
import android.util.LruCache;

public class BitmapCache {
    private final String TAG = "BitmapCache";

    private final int CACHE_SIZE_DIVIDER = 8;
    private static BitmapCache instance;


    private InternalCache cache;

    public static BitmapCache getCache(){
        if(instance==null){
            instance = new BitmapCache();
        }
        return instance;
    }

    protected BitmapCache(){
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / CACHE_SIZE_DIVIDER;
        cache = new InternalCache(cacheSize);
    }

    protected void storeBitmap(String key, Bitmap bitmap) {
        if (getBitmap(key) == null) {
            cache.put(key, bitmap);
        }
    }

    protected Bitmap getBitmap(String key) {
        return cache.get(key);
    }

    protected boolean hasBitmap(String key){
        return getBitmap(key)!=null;
    }

    private class InternalCache extends LruCache<String, Bitmap>{

        public InternalCache(int maxSize) {
            super(maxSize);
            Log.d(TAG, "Created new InternalCache, max size: " + maxSize);
        }

        @Override
        protected int sizeOf(String key, Bitmap bitmap) {
            return bitmap.getByteCount() / 1024;
        }
    }
}
