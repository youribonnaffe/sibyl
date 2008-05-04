package com.sibyl.ui;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDiskIOException;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Menu.Item;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.sibyl.Directory;
import com.sibyl.MusicDB;
import com.sibyl.R;
import com.sibyl.ui.animation.ActivityTransition;

/*
 * display all the cover available. The cover choosen is given to Album and associated with the current album
 */
public class CoverUI extends Activity {

    private MusicDB mdb;    //the database
    private static final String TAG = "CoverUI"; //tag for the ui
    //menu constant
    private static final int BACK_ID = Menu.FIRST;
    private static final int RESET_ID = Menu.FIRST+1;
    public static final String ALBUM_ID = "album_id";
    public static final String ALBUM_NAME = "album_name";
    //File format supported.
    public static final String[] EXT_TAB = { ".jpg", ".bmp", ".png"}; //file format search in directories

    private GridView gallery; //the main view of the ui
    private LinearLayout groupView;
    private ImageAdapter imgAdapter;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Log.v(TAG,"CoverUI is launched");

        setContentView(R.layout.cover);
        groupView = (LinearLayout) findViewById(R.id.group);
        gallery = (GridView) findViewById(R.id.gallery);
        gallery.setOnItemClickListener(galleryClickListenner);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String title = extras.getString(CoverUI.ALBUM_NAME);
            TextView album = (TextView) findViewById(R.id.titre);
            album.setText(title);
        }
        try
        {
            mdb = new MusicDB(this);
            fillData();
        }
        catch(SQLiteDiskIOException ex)
        {
            Log.v(TAG, ex.toString());
        }       
    }

    @Override
    protected void onResume() 
    {
        super.onResume();
        ActivityTransition trans = new ActivityTransition( true );
        trans.setDuration(500);
        trans.setFillAfter(true);
        trans.setInterpolator(new AccelerateInterpolator());
        groupView.startAnimation(trans);
    }
    
    @Override
    protected void onPause() 
    {
        ActivityTransition trans = new ActivityTransition( false );
        trans.setDuration(500);
        trans.setFillAfter(true);
        trans.setInterpolator(new DecelerateInterpolator());
        groupView.startAnimation(trans);
        super.onPause();
    }
    
    /**
     * manage click on cover. If no cover clicked (clicked in the emptyness, send a RESULT_CANCELED)
     */
    private OnItemClickListener galleryClickListenner = new OnItemClickListener()
    {
        public void onItemClick(AdapterView arg0, View arg1, int position, long id) {
            if( position >=0){
                //Log.v(TAG, (String) imgAdapter.getItem(position));
                setResult(RESULT_OK,  (String) imgAdapter.getItem(position));
            }
            else
            {
                setResult(RESULT_CANCELED);
            }
            finish();
        }
    }; 



    @Override
    public boolean onMenuItemSelected(int featureId, Item item) {
        switch(item.getId()){
            case BACK_ID:
                setResult(RESULT_CANCELED);
                finish();
                break;
            case RESET_ID:
                int position = gallery.getSelectedItemPosition();
                if( position >=0){
                    // we want to reset 
                    setResult(RESULT_FIRST_USER);
                    finish();
                }
                break;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    public boolean onCreateOptionsMenu(Menu menu) 
    {
        super.onCreateOptionsMenu(menu);
        menu.add(0, BACK_ID, R.string.cov_back);
        menu.add(0, RESET_ID, R.string.cov_reset);
        return true;
    }

    /**
     * Fill the gridView with the pictures (.jpg, .bmp, .png) found in the directories of the collection
     */
    private void fillData(){
        Cursor c = mdb.getDir();
        imgAdapter = new ImageAdapter(this);
        startManagingCursor(c);
        Log.v(TAG, ">>>CoverUI::filldata ");
        while( c.next()){
            for(String extension : EXT_TAB){
                for(String file : Directory.scanFiles(c.getString(0), extension)){
                    imgAdapter.add(file);
                    Log.v(TAG, "    "+file);
                }
            }
        }
        c.close();        
        gallery.setAdapter(imgAdapter);        
    }

}
