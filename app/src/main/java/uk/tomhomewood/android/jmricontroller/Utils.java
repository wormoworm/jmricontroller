package uk.tomhomewood.android.jmricontroller;

import java.math.BigInteger;
import java.security.MessageDigest;

import android.content.Context;

public class Utils {

	/** ------ copied from jmri util code -------------------
	 * Split a string into an array of Strings, at a particular
	 * divider.  This is similar to the new String.split method,
	 * except that this does not provide regular expression
	 * handling; the divider string is just a string.
	 * @param input String to split
	 * @param divider Where to divide the input; this does not appear in output
	 */
	public static String[] splitStringByString(String input, String divider) {
		int size = 0;
		String temp = input;

		// count entries
		while(temp.length() > 0) {
			size++;
			int index = temp.indexOf(divider);
			if (index < 0) break;    // break not found
			temp = temp.substring(index+divider.length());
			if (temp.length() == 0) {  // found at end
				size++;
				break;
			}
		}

		String[] result = new String[size];

		// find entries
		temp = input;
		size = 0;
		while(temp.length() > 0) {
			int index = temp.indexOf(divider);
			if (index < 0) break;    // done with all but last
			result[size] = temp.substring(0,index);
			temp = temp.substring(index+divider.length());
			size++;
		}
		result[size] = temp;

		return result;
	}

	public static int convertDpToPixels(float dps, Context context){
		float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dps * scale + 0.5f);
	}
	
	public static String generateSHA1(String input) {
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
			messageDigest.update(input.getBytes());
			
			BigInteger hash = new BigInteger(1, messageDigest.digest());
			
			String HashPassString = hash.toString(16);
			
			if ((HashPassString.length() % 2) != 0) {
				HashPassString = "0" + HashPassString;
			}
			return HashPassString;
		} 
		catch (Exception e) {
		}
		return null;
	}
}