package com.sibyl.ui;

import java.util.ArrayList;

import com.sibyl.MusicDB;
import com.sibyl.R;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.Menu.Item;
import android.widget.ArrayAdapter;

public class DelDirUI extends ListActivity
{

    private static final int DEL_ID = Menu.FIRST;
    private static final int BACK_ID = Menu.FIRST +1;
    
    private static final String TAG = "DEL_DIR";

    private MusicDB mdb;    //the database
    
    public void onCreate(Bundle icicle) 
    {
        super.onCreate(icicle);
        try
        {
            mdb = new MusicDB(this);
            Log.v(TAG,"BD OK");
        }
        catch(Exception ex)
        {
            Log.v(TAG, ex.toString()+" Create");
        }   
        ArrayList<String> mStrings = fillBD();
        setListAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, mStrings));
    }
    private ArrayList<String> fillBD ()
    {
        ArrayList<String> listDir = new ArrayList<String>();
        Cursor c = mdb.getDir();
        while (c.next())
        {
            Log.v(TAG,"ADD !"+c.getString(0));
            listDir.add(c.getString(0));
        }
        return listDir;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        super.onCreateOptionsMenu(menu);
        menu.add(0, DEL_ID, R.string.menu_del);
        menu.add(0, BACK_ID, R.string.menu_back);
        return true;
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, Item item) 
    {
        super.onMenuItemSelected(featureId, item);
        switch(item.getId()) 
        {
        case DEL_ID:
            int i = getSelectedItemPosition();
            i++;
            Log.v(TAG,""+i);
            mdb.delDir(""+i);
            finish();
            break;
        case BACK_ID:
            finish();
            break;
        }
        return true;
    }
}
