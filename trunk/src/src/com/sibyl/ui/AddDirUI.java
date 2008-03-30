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

import java.io.File;
import java.util.ArrayList;

import android.app.ListActivity;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Menu.Item;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.sibyl.MusicDB;
import com.sibyl.R;

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
	setContentView(R.layout.add);
	path = "/data/music";
	mStrings = fillBD(path);
	setListAdapter(new ArrayAdapter<String>(this,R.layout.add_row,R.id.text1, mStrings));
	try
	{
	    mdb = new MusicDB(this);
	}
	catch(SQLiteDiskIOException ex)
	{
	    Log.v(TAG, ex.toString());
	}   
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) 
    {
	path = mStrings.get(position);
	mStrings = fillBD(path);
	setListAdapter(new ArrayAdapter<String>(this,
		R.layout.add_row,R.id.text1, mStrings));
    }

//  Fill the table Song with mp3 found in path
    private ArrayList<String> fillBD (String path)
    {
	ArrayList<String> str = new ArrayList<String>();
	// get all mp3 files in path
	File dir = new File(path);
	String parent = dir.getParent();
	// might have to check !=null
	if (parent != null)
	{
	    str.add(parent);
	}

	//Log.v(TAG,"taille dir : "+dir.list().length);
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
		Log.v(TAG,sqle.toString());
	    }
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
