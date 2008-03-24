package com.sibyl.ui;

import java.io.File;
import java.util.ArrayList;

import com.sibyl.MusicDB;
import com.sibyl.R;
import android.app.ListActivity;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Menu.Item;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class AddDirUI extends ListActivity
{
    private static final int ADD_ID = Menu.FIRST;
    private static final int BACK_ID = Menu.FIRST +1;
    
    private static final String TAG = "ADD_DIR";
    private ArrayList<String> mStrings;
    private String path;

    private MusicDB mdb;    //the database
    
    public void onCreate(Bundle icicle) 
    {
        super.onCreate(icicle);
        path = "/data/music";
        mStrings = fillBD(path);
        setListAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, mStrings));
        try
        {
            mdb = new MusicDB(this);
            Log.v(TAG,"BD OK");
        }
        catch(Exception ex)
        {
            Log.v(TAG, ex.toString()+" Create");
        }   
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) 
    {
        path = mStrings.get(position);
        mStrings = fillBD(path);
        setListAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, mStrings));
    }
    
//  Fill the table Song with mp3 found in path
    private ArrayList<String> fillBD (String path)
    {
        ArrayList<String> str = new ArrayList<String>();
        // get all mp3 files in path
        try
        {
            File dir = new File(path);
            String parent = dir.getParent();
            if (parent != null)
            {
                str.add(parent);
            }
            
            Log.v(TAG,"taille dir : "+dir.list().length);
            for(File f: dir.listFiles())
            {
                try
                {
                    if (f.isDirectory())
                    {
                        str.add(f.getPath());
                    }
                }
                catch(SQLiteException sqle)
                {
                    Log.v(TAG, "sql" + sqle.toString());
                }
            }
        }
        catch(Exception ex)
        {
            Log.v(TAG, ex.toString());
        }
        return str;
    }
    
    @Override
    protected void onDestroy() 
    {
        super.onDestroy();     
    }
    
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        super.onCreateOptionsMenu(menu);
        menu.add(0, ADD_ID, R.string.menu_add);
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
            Log.v(TAG, "Insert");
            Log.v(TAG,"Ajout dans la table du repertoire :"+path);
            mdb.insertDir(path);
            finish();
            break;
        case BACK_ID:
            finish();
            break;
        }
        
        return true;
    }
}
