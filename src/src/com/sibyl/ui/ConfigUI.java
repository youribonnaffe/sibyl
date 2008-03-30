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
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Menu.Item;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.sibyl.MusicDB;
import com.sibyl.R;

public class ConfigUI extends Activity
{
    private EditText mText;
    private Button addMusic;
    private Button delMusic;
    private Button updateMusic;
    private ArrayList<String> listFile;
    private static final int BACK_ID = Menu.FIRST;
    
    private static final String TAG = "CONFIG";
    
    private MusicDB mdb;    //the database
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) 
    {
        super.onCreate(icicle);
        setContentView(R.layout.config);
        mText = (EditText) findViewById(R.id.musicData);
        addMusic = (Button) findViewById(R.id.addMusic);
        delMusic = (Button) findViewById(R.id.delMusic);
        updateMusic = (Button) findViewById(R.id.updateMusic);
        addMusic.setOnClickListener(mAddMusic);
        delMusic.setOnClickListener(mDelMusic);
        updateMusic.setOnClickListener(mUpdateMusic);
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
    protected void onStart()
    {
        super.onStart();
        listFile = new ArrayList<String>();
        Cursor c = mdb.getDir();
        while (c.next())
        {
            //Log.v(TAG,"ADD !"+c.getString(0));
            listFile.add(c.getString(0));
        }
        String str = "";
        for (int i = 0; i < listFile.size(); i++)
        {
            str += listFile.get(i)+'\n';
        }
        mText.setText(str);
    }
    
    private void displayAddDir() 
    {
        Intent i = new Intent(this, AddDirUI.class);
        startSubActivity(i, 0);
    }
    
    private void displayDelDir() 
    {
        Intent i = new Intent(this, DelDirUI.class);
        startSubActivity(i, 0);
    }
    
    private OnClickListener mAddMusic = new OnClickListener()
    {
        public void onClick(View v)
        {
            displayAddDir();
            /*
            Log.v(TAG,"VALIDATION REP");
            StringTokenizer str = new StringTokenizer(mText.getText().toString(),";");
            while (str.hasMoreTokens())
            {
                String token = str.nextToken();
                Log.v(TAG,"répertoire : "+token);
                listFile.add(token);
            }*/
        }
    };
    
    private OnClickListener mDelMusic = new OnClickListener()
    {
        public void onClick(View v)
        {
            displayDelDir();
        }
    };
    
    private OnClickListener mUpdateMusic = new OnClickListener()
    {
        public void onClick(View v)
        {
            mdb.clearDB();
            for(int i=0; i<listFile.size(); i++)
            {
                fillBD(listFile.get(i));
            }
        }
    };
    
    //  Fill the table Song with mp3 found in path
    private void fillBD (String path)
    {
        // get all mp3 files in path
        try
        {
            path += '/';
            File dir = new File(path);
            //Log.v(TAG, "Insert"+path);
            FilenameFilter filter = new FilenameFilter() 
            {
                public boolean accept(File dir, String name) 
                {
                    return name.endsWith(".mp3"); // extension should be a parameter
                }
            };

            // insert them in the database    
            for(String s : dir.list(filter))
            {
        	try{
                    // ?? long t = System.currentTimeMillis();
                    mdb.insert(path+s);
                    //Log.v(TAG, "temps "+(System.currentTimeMillis()-t));
        	}catch(SQLiteException sqle){
                    Log.v(TAG, sqle.toString());
                    // warn user
        	}catch(FileNotFoundException fnfe){
                    Log.v(TAG, fnfe.toString());
                    // warn user
        	}catch(IOException ioe){
                    Log.v(TAG, ioe.toString());
                    // warn user
        	}        	
            }
        }
        catch(NullPointerException ex)
        {
           // will never happen
        }
    }
    
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        super.onCreateOptionsMenu(menu);
        menu.add(0, BACK_ID, R.string.menu_back);
        return true;
    }
    
    @Override
    protected void onDestroy() 
    {
        super.onDestroy();     
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
        }
        return true;
    }

}
