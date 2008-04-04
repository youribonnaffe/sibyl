/* 
 *
 * Copyright (C) 2007-2008 sibyl project
 * http://code.google.com/p/sibyl/
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sibyl.ui;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDiskIOException;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Menu.Item;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.sibyl.MusicDB;
import com.sibyl.R;

public class DelDirUI extends ListActivity
{

    private static final int DEL_ID = Menu.FIRST;
    private static final int BACK_ID = Menu.FIRST +1;
    
    private static final String TAG = "DEL_DIR";

    private MusicDB mdb;    //the database
    
    private ArrayList<String> mStrings = new ArrayList<String>();
    
    public void onCreate(Bundle icicle) 
    {
        super.onCreate(icicle);
        setContentView(R.layout.del_dir);
        try
        {
            mdb = new MusicDB(this);
            fillBD();
            setListAdapter(new ArrayAdapter<String>(this,R.layout.del_dir_row,R.id.text1, mStrings));
        }
        catch(SQLiteDiskIOException ex)
        {
            Log.v(TAG, ex.toString() + "MERDE !");
        }   
    }
    
    private void fillBD ()
    {
        ArrayList<String> listDir = mStrings;
        Cursor c = mdb.getDir();
        while (c.next())
        {
            Log.v(TAG,"ADD !"+c.getString(0));
            listDir.add(c.getString(0));
        }
        //return listDir;
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
            Log.v(TAG,""+i);
            mdb.delDir(mStrings.get(i));
            finish();
            break;
        case BACK_ID:
            finish();
            break;
        }
        return true;
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, final int position, long id) 
    {
        //Log.v(TAG,"test <<<<<<<<<<<< >>>>>>>>>>>>>"+position); 
        new AlertDialog.Builder(DelDirUI.this)
                .setIcon(R.drawable.play)
                .setTitle(R.string.dial_deldir)
                .setPositiveButton("yes", new DialogInterface.OnClickListener() 
                {
                    public void onClick(DialogInterface dialog, int whichButton) 
                    {
                        int i = position;
                        //Log.v(TAG,""+i);
                        mdb.delDir(mStrings.get(i));
                        finish();
                    }
                })
                .setNegativeButton("no", new DialogInterface.OnClickListener() 
                {
                    public void onClick(DialogInterface dialog, int whichButton) 
                    {
                    }
                })
                .show();
    }
}
