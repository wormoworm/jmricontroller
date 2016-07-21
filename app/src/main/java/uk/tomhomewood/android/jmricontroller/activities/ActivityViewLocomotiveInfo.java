package uk.tomhomewood.android.jmricontroller.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import uk.tomhomewood.android.jmricontroller.BitmapUtils;
import uk.tomhomewood.android.jmricontroller.ImageLoader;
import uk.tomhomewood.android.jmricontroller.Locomotive;
import uk.tomhomewood.android.jmricontroller.R;

public class ActivityViewLocomotiveInfo extends BaseActivity {
    private final String TAG = "ActivityViewLocomotiveInfo";

    private static final String EXTRA_LOCOMOTIVE = "locomotive";

    public static void launch(Activity activity, Locomotive locomotive){
        Intent intent = new Intent(activity, ActivityViewLocomotiveInfo.class);
        intent.putExtra(EXTRA_LOCOMOTIVE, locomotive);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_locomotive_info);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        Locomotive locomotive = (Locomotive) getIntent().getSerializableExtra(EXTRA_LOCOMOTIVE);
        if(locomotive!=null) {
            initialiseUi(locomotive);
        }
    }

    private void initialiseUi(final Locomotive locomotive) {
        if(locomotive.hasImage()) {
            final ImageView image = (ImageView) findViewById(R.id.activity_view_locomotive_info_image);

            ViewTreeObserver viewTree = image.getViewTreeObserver();
            viewTree.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                public boolean onPreDraw() {
                    new ImageLoader().loadImage(locomotive.getImagePath(), image, AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in));
                    image.getViewTreeObserver().removeOnPreDrawListener(this);
                    return true;
                }
            });
        }

        TextView number = (TextView) findViewById(R.id.activity_view_locomotive_info_number);
        number.setText(locomotive.getNumber());

        setStringInfo(R.id.activity_view_locomotive_info_manufacturer_block, locomotive.getManufacturer());
        setStringInfo(R.id.activity_view_locomotive_info_model_block, locomotive.getModel());
        setStringInfo(R.id.activity_view_locomotive_info_owner_block, locomotive.getOwner());
        setStringInfo(R.id.activity_view_locomotive_info_address_block, locomotive.getAddress());
    }

    private void setStringInfo(int infoBlockResId, String text){
        LinearLayout block = (LinearLayout) findViewById(infoBlockResId);
        block.setVisibility(isSet(text)? View.VISIBLE : View.GONE);
        if(isSet(text)){
            TextView blockText = (TextView) block.getChildAt(1);
            blockText.setText(text);
        }
    }

    private boolean isSet(String text) {
        return text!=null && !text.isEmpty();
    }
}
