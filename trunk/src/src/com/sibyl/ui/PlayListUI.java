package com.sibyl.ui;

import java.util.ArrayList;

import com.sibyl.MusicDB;
import com.sibyl.R;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Contacts.People;
import android.provider.Contacts.Phones;
import android.util.Log;
import android.view.Menu;
import android.view.Menu.Item;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class PlayListUI extends ListActivity
{
    private static final int ADD_ID = Menu.FIRST;
    private static final int NEW_ID = Menu.FIRST +1;
    private static final int SMARTPL_ID = Menu.FIRST +2;
    private static final int BACK_ID = Menu.FIRST +3;
    
    private static final String TAG = "PLAYLIST";
    
    private TextView playList;
    
    private MusicDB mdb;    //the database
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) 
    {
        super.onCreate(icicle);
        setContentView(R.layout.playlist);
        playList = (TextView) findViewById(R.layout.playlist);
        try
        {
            mdb = new MusicDB(this);
            Log.v(TAG,"BD OK");
        }
        catch(Exception ex)
        {
            Log.v(TAG, ex.toString()+" Create");
        }   
        fillData();
    }
    
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        super.onCreateOptionsMenu(menu);
        menu.add(0, ADD_ID, R.string.menu_add);
        menu.add(0, NEW_ID, R.string.menu_new);
        menu.add(0, SMARTPL_ID, R.string.menu_smartpl);
        menu.add(0, BACK_ID, R.string.menu_back);
        return true;
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, Item item) 
    {
        super.onMenuItemSelected(featureId, item);
        switch(item.getId()) 
        {
        case ADD_ID:
            break;
        case NEW_ID:
            break;
        case SMARTPL_ID:
            break;
        case BACK_ID:
            finish();
            break;
        }
        
        return true;
    }
    
    private void fillData()
    {
        try
        {
            Log.v(TAG,"Curseur creation <<<<<<<<<<<<<<<<");
            Cursor c = mdb.rawQuery("SELECT  title _id , artist_name " +
                    "FROM song, artist, current_playlist " +
                    "WHERE song.artist = artist.id AND " +
                    "current_playlist.id = song._id", null);

            startManagingCursor(c);

            try{
            ListAdapter adapter = new SimpleCursorAdapter(
                    this, R.layout.playlist_row, c, new String[] {"_id","artist_name"}, 
                    new int[] {R.id.text1, R.id.text2});  
            setListAdapter(adapter);
            }
            catch(Exception ex)
            {
                Log.v(TAG, ex.toString());
            }
        }
        catch(Exception ex)
        {
            Log.v(TAG, ex.toString());
        }
        
    }

    
}
