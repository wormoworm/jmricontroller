package uk.tomhomewood.android.jmricontroller.activities;

import java.util.ArrayList;

import uk.tomhomewood.android.jmricontroller.BitmapUtils;
import uk.tomhomewood.android.jmricontroller.Database;
import uk.tomhomewood.android.jmricontroller.ImageCache;
import uk.tomhomewood.android.jmricontroller.ImageLoader;
import uk.tomhomewood.android.jmricontroller.Locomotive;
import uk.tomhomewood.android.jmricontroller.R;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.util.LruCache;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

public class ActivityChooseLocomotive extends BaseActivity implements OnItemClickListener, OnItemLongClickListener {
    private final String TAG = "ActivityChooseLocomotive";

    public static final String EXTRA_MULTI_SELECT = "multiSelect";
    public static final String EXTRA_LOCOMOTIVE = "locomotive";
    public static final String EXTRA_LOCOMOTIVES_LIST = "locomotivesList";

    private final String SAVED_STATE_SELECTED_LOCOMOTIVES = "selectedLocomotives";

    private GridView locomotivesGrid;
    private LocomotivesAdapter locomotivesAdapter;

    private ArrayList<Locomotive> selectedLocomotives;

    private boolean multiSelect;

    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        forceManualOrientation();
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_choose_locomotive);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        locomotivesGrid = (GridView) findViewById(R.id.activity_choose_locomotive_list);
        locomotivesAdapter = new LocomotivesAdapter(this);
        locomotivesGrid.setAdapter(locomotivesAdapter);
        locomotivesGrid.setOnItemClickListener(this);
        locomotivesGrid.setOnItemLongClickListener(this);

        Intent launchIntent = getIntent();
        multiSelect = launchIntent.getBooleanExtra(EXTRA_MULTI_SELECT, false);
        if(multiSelect){
            if(savedInstanceState!=null){
                selectedLocomotives = (ArrayList<Locomotive>) savedInstanceState.getSerializable(SAVED_STATE_SELECTED_LOCOMOTIVES);
            }
            if(selectedLocomotives==null){
                selectedLocomotives = new ArrayList<Locomotive>();
            }
            locomotivesGrid.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE);
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_choose_locomotive, menu);
        if(!multiSelect){
            menu.removeItem(R.id.menu_action_done);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case android.R.id.home:
                finish();
                break;
            case R.id.menu_action_done:
                Intent returnIntent = new Intent();
                returnIntent.putExtra(EXTRA_LOCOMOTIVES_LIST, selectedLocomotives);
                setResult(RESULT_OK, returnIntent);
                finish();
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState){
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putSerializable(SAVED_STATE_SELECTED_LOCOMOTIVES, selectedLocomotives);
    }

    @Override
    public void onItemClick(AdapterView<?> adapter, View item, int position, long itemId) {
        Locomotive selectedLocomotive = locomotivesAdapter.getItem(position);
        if(multiSelect){
            if(selectedLocomotives.contains(selectedLocomotive)){
                selectedLocomotives.remove(selectedLocomotive);
            }
            else{
                selectedLocomotives.add(selectedLocomotive);
            }
        }
        else{
            Intent returnIntent = new Intent();
            returnIntent.putExtra(EXTRA_LOCOMOTIVE, selectedLocomotive);
            setResult(RESULT_OK, returnIntent);
            finish();
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapter, View item, int position, long itemId) {
        return false;
    }

    class LocomotivesAdapter extends BaseAdapter{
        private ArrayList<Locomotive> locomotives;
        private Database database;
//        private ArrayList<String> bitmapsLoaded;
        private Animation imageLoadedAnimation;

        private Integer itemWidth, itemHeight;

        private ImageLoader imageLoader;

        public LocomotivesAdapter(Context context){
            imageLoader = new ImageLoader();
            imageLoadedAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
            database = new Database(context);
//            bitmapsLoaded = new ArrayList<String>();
            loadLocomotiveList();
        }

        private void loadLocomotiveList() {
            locomotives = database.getLocomotives();
        }

        @Override
        public int getCount() {
            return locomotives.size();
        }

        @Override
        public Locomotive getItem(int position) {
            return locomotives.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        public int indexOf(Locomotive locomotive){
            return locomotives.indexOf(locomotive);
        }

        public boolean contains(Locomotive locomotive){
            return locomotives.contains(locomotive);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            ViewHolder viewHolder;
            if(view==null){		//True if we do not have a view to re-use
                viewHolder = new ViewHolder();
                LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = vi.inflate(R.layout.activity_choose_locomotive_grid_item, parent, false);
                viewHolder.image = (ImageView) view.findViewById(R.id.activity_choose_locomotive_grid_item_image);
                viewHolder.number = (TextView) view.findViewById(R.id.activity_choose_locomotive_grid_item_number);
                viewHolder.description = (TextView) view.findViewById(R.id.activity_choose_locomotive_grid_item_description);
                view.setTag(viewHolder);
            }
            else{
                viewHolder = (ViewHolder) view.getTag();
            }
            Locomotive locomotive = getItem(position);
            viewHolder.number.setText(locomotive.getNumber());
            viewHolder.description.setText(locomotive.getModel());

            if(itemWidth==null && itemHeight==null){            //No child has yet been measured
                int measuredWidth = viewHolder.image.getMeasuredWidth();
                int measuredHeight = viewHolder.image.getMeasuredHeight();
                if(measuredWidth!=0 && measuredHeight!=0){      //The child has a width
                    itemWidth = measuredWidth;
                    itemHeight = measuredHeight;
                }
            }

            if(itemDimensionsKnown()) {
                if (locomotive.hasImage()) {

                    loadLocomotiveImage(locomotive, viewHolder.image);
                } else {
                    loadDefaultLocomotiveImage(viewHolder.image);
                }
            }

            return view;
        }

        private boolean itemDimensionsKnown(){
            return itemWidth!=null && itemHeight!=null;
        }

        private void loadLocomotiveImage(Locomotive locomotive, ImageView imageView) {
            if(!imageLoader.isLoadingImage(locomotive.getImagePath(), imageView)) {
                imageLoader.loadImage(locomotive.getImagePath(), itemWidth, itemHeight, imageView, imageLoadedAnimation);
            }
        }

        private void loadDefaultLocomotiveImage(ImageView imageView) {
            imageView.setImageResource(R.drawable.loco_default);
        }
/*
        private void loadingBitmap(String path) {
//            bitmapsLoaded.add(path);
        }

        private boolean isLoadingBitmap(String path){
            return bitmapsLoaded.contains(path);
        }
*/
        class ViewHolder{
            public ImageView image;
            public TextView number, description;
        }
    }
}