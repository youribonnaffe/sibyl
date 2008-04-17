package com.sibyl.ui;


import android.app.ListActivity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDiskIOException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.Menu.Item;

import com.sibyl.MusicDB;
import com.sibyl.R;

public class AlbumUI extends ListActivity {

    private static final String TAG = "ALBUMUI";
    private MusicDB mdb;    //the database
    //constants for the cursor.
    private final static int ARTIST = 0;
    private final static int ALBUM = 1;
    private final static int COVER_URL = 2;
    //constant menu
    private static final int BACK_ID = Menu.FIRST;
    private static final int MANUAL_ID = Menu.FIRST+1;
    
    @Override
    protected void onCreate(Bundle icicle) {
        // TODO Auto-generated method stub
        super.onCreate(icicle);
        Log.v(TAG,"CoverUI is launched");
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
    
    
    private void fillData(){
        
        Cursor c = mdb.getAlbumCover();
        AlbumCoverListAdapter rows = new AlbumCoverListAdapter(this);
        
        startManagingCursor(c);
        while(c.next()){
            Log.v(TAG,c.getString(ARTIST)+"-"+c.getString(ALBUM));
            
            if( c.isNull(COVER_URL)){
                rows.add(new IconifiedText( c.getString(ARTIST)+'\n'+c.getString(ALBUM),
                        getResources().getDrawable(R.drawable.logo)));
            }
            else{
                rows.add(new IconifiedText( c.getString(ARTIST)+'\n'+c.getString(ALBUM),
                        Drawable.createFromPath(c.getString(COVER_URL))));
            }
        }
        c.close();        
        setListAdapter(rows);
    }
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        super.onCreateOptionsMenu(menu);
        menu.add(0, BACK_ID, R.string.alb_back);
        menu.add(0, MANUAL_ID, R.string.alb_manual);
        return true;
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, Item item) 
    {
        super.onMenuItemSelected(featureId, item);
        switch(item.getId()) 
        {
            case BACK_ID:
                finish();
                break;
            case MANUAL_ID:
                break;
        }
        return true;
    }

}
