package uk.tomhomewood.android.jmricontroller.activities;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.ViewConfiguration;
import android.widget.TextView;

import java.lang.reflect.Field;

import uk.tomhomewood.android.jmricontroller.R;

public class BaseActivity extends ActionBarActivity {

    private boolean forceManualOrientation = false;

    protected int SCREEN_TYPE_PHONE;
    protected int SCREEN_TYPE_TABLET_SMALL;
    protected int SCREEN_TYPE_TABLET_LARGE;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        SCREEN_TYPE_PHONE = getResources().getInteger(R.integer.phone);
        SCREEN_TYPE_TABLET_SMALL = getResources().getInteger(R.integer.tablet_small);
        SCREEN_TYPE_TABLET_LARGE = getResources().getInteger(R.integer.tablet_large);

        if(!isLargeTablet() && !forceManualOrientation){				//True if the device is not a tablet (meaning it has a screen smaller than a 7" tablet)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);			//If this is a small device or if this behaviour has been disabled, lock the orientation to portrait
        }
    }

    /**
     * Disables the automatic portrait /landscape behaviour described in {@link BaseActivity#isLargeTablet()}.
     * This must be called before called super.onCreate().
     */
    protected void forceManualOrientation(){
        forceManualOrientation = true;
    }

    /**
     * Helper method to determine if the device has an large screen. For example, 7" tablets are large.
     */
    protected boolean isLargeTablet() {
        return (getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    protected int getScreenType() {
        return getResources().getInteger(R.integer.screen_type);
    }

    /**
     * Forces this {@link BaseActivity} to show the action bar overflow indicator, even if the device has a physical menu button.
     * @return 		True if the overflow indicator was enabled, false otherwise.
     */
    protected boolean forceActionBarOverflow(){
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if(menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
                return true;
            }
            else{
                return false;
            }
        }
        catch (Exception ex) {
            return false;
        }
    }
}