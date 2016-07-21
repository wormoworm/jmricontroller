package uk.tomhomewood.android.jmricontroller;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

public class ImageCache {
	
	private static final int cacheSize = 4 * 1024 * 1024;

	private static LruCache<String, Bitmap> cache;
	
	public static LruCache<String, Bitmap> getCache(){
		if(cache==null){
			cache = new LruCache<String, Bitmap>(cacheSize);
		}
		return cache;
	}
}