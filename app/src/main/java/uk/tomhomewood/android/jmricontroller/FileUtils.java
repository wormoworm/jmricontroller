package uk.tomhomewood.android.jmricontroller;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.util.Log;

public class FileUtils {
	private final static String TAG = "FileUtils";

	public static final String DIRECTORY_LOCOMOTIVE_IMAGES = "locomotiveImages";
	/**
	 * Checks to see if the necessary directories exist in the device's filesystem. They are created if they do not exist
	 */
	public static void checkFilesystem(Context context) {
		checkDirectoryExists(getBaseDirectory(context), true);
		checkDirectoryExists(getSubDirectory(context, DIRECTORY_LOCOMOTIVE_IMAGES), true);
		checkFileExists(new File(getBaseDirectory(context), ".nomedia"), true);
	}

	public static File getBaseDirectory(Context context){
		return new File(context.getExternalFilesDir(null).getAbsolutePath());
	}
	
	public static File getSubDirectory(Context context, String directoryName){
		return new File(getBaseDirectory(context), directoryName);
	}
	
	/**
	 * Checks if the directory specified by the provided path exists. If not, and the parameter createIfNotPresent is true, it is created
	 * @param path					The path to check
	 * @param createIfNotPresent	Whether or not the directory should be created if it does not exist
	 * @return						Whether or not this directory exists. If a directory was successfully created, this will be true to reflect this
	 */
	public static boolean checkDirectoryExists(String path, boolean createIfNotPresent){
		boolean directoryExists = true;
		File directory = new File(path);
		if(!directory.exists()){	//True if the directory does not exist
			directoryExists = false;
			if(createIfNotPresent){
				directory.mkdir();
				directoryExists = true;
			}
		}
		return directoryExists;
	}
	
	/**
	 * Checks if the directory specified by the provided path exists. If not, and the parameter createIfNotPresent is true, it is created
	 * @param directory				The directory to check
	 * @param createIfNotPresent	Whether or not the directory should be created if it does not exist
	 * @return						Whether or not this directory exists. If a directory was successfully created, this will be true to reflect this
	 */
	public static boolean checkDirectoryExists(File directory, boolean createIfNotPresent){
		return checkDirectoryExists(directory.getAbsolutePath(), createIfNotPresent);
	}
	
	/**
	 * Checks if the file specified by the provided path exists. If not, and the parameter createIfNotPresent is true, an empty file is created
	 * @param path					The path to check
	 * @param createIfNotPresent	Whether or not the file should be created if it does not exist
	 * @return						Whether or not this file exists. If an empty file was successfully created, this will be true to reflect this
	 */
	public static boolean checkFileExists(String path, boolean createIfNotPresent){
		boolean fileExists = true;
		File file = new File(path);
		if(!file.exists()){	//True if the directory does not exist
			fileExists = false;
			if(createIfNotPresent){
				try {
					file.createNewFile();
					fileExists = true;
				}
				catch (IOException e) {
					Log.e(TAG, "Error creating file: "+e.toString());
				}
			}
		}
		return fileExists;
	}
	
	public static void checkFileExists(File file, boolean createIfNotPresent) {
		checkFileExists(file.getAbsolutePath(), createIfNotPresent);
	}
}
