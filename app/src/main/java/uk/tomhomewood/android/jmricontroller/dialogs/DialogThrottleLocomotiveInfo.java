package uk.tomhomewood.android.jmricontroller.dialogs;

import uk.tomhomewood.android.jmricontroller.BitmapUtils;
import uk.tomhomewood.android.jmricontroller.ImageCache;
import uk.tomhomewood.android.jmricontroller.Locomotive;
import uk.tomhomewood.android.jmricontroller.R;
import uk.tomhomewood.android.jmricontroller.network.WiThrottleCommandClientThrottle;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class DialogThrottleLocomotiveInfo extends Dialog implements android.view.View.OnClickListener{

	private WiThrottleCommandClientThrottle commandClient;
	private Locomotive locomotive;

	private ImageView locomotiveImage;
	private TextView locomotiveNumber;
	private TextView locomotiveModel;
	private Button buttonRelease;

	public DialogThrottleLocomotiveInfo(WiThrottleCommandClientThrottle commandClient, Context context) {
		super(context);
		this.commandClient = commandClient;

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.dialog_throttle_locomotive_info);
		getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
		getWindow().getAttributes().dimAmount = 0.8f;
		
		locomotiveImage = (ImageView) findViewById(R.id.dialog_throttle_locomotive_info_image);
		buttonRelease = (Button) findViewById(R.id.dialog_throttle_locomotive_info_button_release);
		locomotiveNumber = (TextView) findViewById(R.id.dialog_throttle_locomotive_info_number);
		locomotiveModel = (TextView) findViewById(R.id.dialog_throttle_locomotive_info_model);

		buttonRelease.setOnClickListener(this);

		if(commandClient!=null){
			locomotive = commandClient.getLocomotive();
		}
		if(locomotive!=null){
			int imageSize = context.getResources().getDimensionPixelSize(R.dimen.dialog_throttle_locomotive_info_width);
			String locomotiveImagePath = locomotive.getImagePath();
			if(locomotiveImagePath!=null){
				new BitmapUtils.LoadBitmapAsync(locomotiveImagePath, locomotiveImage, imageSize, imageSize, true, ImageCache.getCache()).execute();
			}
			locomotiveNumber.setText(locomotive.getNumber());
			if(locomotive.getModel()!=null){
				locomotiveModel.setText(locomotive.getModel());
			}
		}

		setCanceledOnTouchOutside(false);
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()){
		case R.id.dialog_throttle_locomotive_info_button_release:
			commandClient.releaseLocomotive();
			dismiss();
			break;
		default:
			break;
		}
	}
}
