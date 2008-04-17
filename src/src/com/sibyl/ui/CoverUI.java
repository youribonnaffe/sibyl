package com.sibyl.ui;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDiskIOException;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Menu.Item;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

import com.sibyl.Directory;
import com.sibyl.MusicDB;
import com.sibyl.R;

/*
 * display all the cover available. The cover choosen is given to Album and associated with the current album
 */
public class CoverUI extends Activity {

    private MusicDB mdb;    //the database
    private static final String TAG = "CoverUI"; //tag for the ui
    //menu constant
    private static final int BACK_ID = Menu.FIRST;
    private static final int CHOOSE_ID = Menu.FIRST+1;
    
    //File format supported.
    private static final int NB_EXT = 3; //number of file formats
    private static final String[] extTab= { ".jpg", ".bmp", ".png"}; //file format search in directories
    
    private GridView gallery = null; //the main view of the ui
    private ImageAdapter imgAdapter;
    
    @Override
    protected void onCreate(Bundle icicle) {
        // TODO Auto-generated method stub
        Log.v(TAG,"CoverUI is launched");
        
        setContentView(R.layout.cover);
        gallery = (GridView) findViewById(R.id.gallery);
        gallery.setOnItemClickListener(galleryClickListenner);
        try
        {
            mdb = new MusicDB(this);
            fillData();
        }
        catch(SQLiteDiskIOException ex)
        {
            Log.v(TAG, ex.toString());
        }       
 
        super.onCreate(icicle);
    }
    
    /*
     * manage click on cover. If no cover clicked (clicked in the emptyness, send a RESULT_CANCELED)
     */
    private OnItemClickListener galleryClickListenner = new OnItemClickListener()
    {
        public void onItemClick(AdapterView arg0, View arg1, int position, long id) {
            if( position >=0){
                Log.v(TAG, (String) imgAdapter.getItem(position));
                setResult(RESULT_OK,  (String) imgAdapter.getItem(position));
                finish();
            }
            else
            {
                setResult(RESULT_CANCELED);
                finish();
            }
        }
    }; 

    

    @Override
    public boolean onMenuItemSelected(int featureId, Item item) {
        switch(item.getId()){
            case BACK_ID:
                setResult(RESULT_CANCELED);
                finish();
                break;
            case CHOOSE_ID:
                int position = gallery.getSelectedItemPosition();
                if( position >=0){
                    Log.v(TAG, (String) imgAdapter.getItem(position));
                    setResult(RESULT_OK,  (String) imgAdapter.getItem(position));
                    finish();
                }
                else
                {
                    setResult(RESULT_CANCELED);
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
        menu.add(0, CHOOSE_ID, R.string.cov_choose);
        return true;
    }
    
    /*
     * Fill the gridView with the pictures (.jpg, .bmp, .png) found in the directories of the collection
     */
    private void fillData(){
        Cursor c = mdb.getDir();
        imgAdapter = new ImageAdapter(this);
        startManagingCursor(c);
        while( c.next()){
            for(String file : Directory.scanFiles(c.getString(0), extTab, NB_EXT)){
                imgAdapter.add(file);
            }
        }
        c.close();        
        gallery.setAdapter(imgAdapter);        
    }
    
}
