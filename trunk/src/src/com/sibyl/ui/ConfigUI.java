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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Menu.Item;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.sibyl.Directory;
import com.sibyl.ISibylservice;
import com.sibyl.MusicDB;
import com.sibyl.R;
import com.sibyl.Sibylservice;

public class ConfigUI extends Activity
{
    ISibylservice mService = null;
    
    private EditText mText;
    private Button addMusic;
    private Button delMusic;
    private Button updateMusic;
    private Button repeatMusic;
    private ArrayList<String> listFile;
    private static final int BACK_ID = Menu.FIRST;
    private int repeatMod = 0; 
    private static final String TAG = "CONFIG";
    
    private static final String PREF_NAME = "SibylPref";
    
    private MusicDB mdb;    //the database
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) 
    {
        super.onCreate(icicle);
        setContentView(R.layout.config);
        launchService();
        mText = (EditText) findViewById(R.id.musicData);
        addMusic = (Button) findViewById(R.id.addMusic);
        delMusic = (Button) findViewById(R.id.delMusic);
        updateMusic = (Button) findViewById(R.id.updateMusic);
        repeatMusic = (Button) findViewById(R.id.repMusic);
        addMusic.setOnClickListener(mAddMusic);
        delMusic.setOnClickListener(mDelMusic);
        updateMusic.setOnClickListener(mUpdateMusic);
        repeatMusic.setOnClickListener(mRepeatMusic);
        repeatMusic.setFocusable(false);
        try
        {
            mdb = new MusicDB(this);
        }
        catch(SQLiteDiskIOException ex)
        {
            Log.v(TAG, ex.toString());
        }
        
        ArrayList<String> al = Directory.scanFiles("/data/music/",".mp3");
        for(String s : al){
            Log.v("LIST", s);
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
    
    @Override
    protected void onStop()
    {
        super.onStart();

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
    
    private OnClickListener mRepeatMusic = new OnClickListener()
    {
        public void onClick(View v)
        {
            repeatMod = (repeatMod + 1) % 3;
            try 
            {
                switch(repeatMod)
                {
                case 0 :
                    repeatMusic.setText(R.string.rep_no);
                    mService.setLooping(false);
                    break;
                case 1 :
                    repeatMusic.setText(R.string.rep_one);
                    mService.setLooping(true);
                    break;
                case 2 :
                    repeatMusic.setText(R.string.rep_all);
                    mService.setRepeatAll();
                    break;
                }
            } catch (DeadObjectException e) { }
        }
    };
    
    public void launchService() 
    {
        bindService(new Intent(ConfigUI.this,
            Sibylservice.class), mConnection, Context.BIND_AUTO_CREATE);
    }
    
    private ServiceConnection mConnection = new ServiceConnection()
    {
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            mService = ISibylservice.Stub.asInterface((IBinder)service);
            repeatMusic.setFocusable(true);
            SharedPreferences settings = getSharedPreferences(PREF_NAME,0);
            int repeatMod = settings.getInt("repeatMod",0);
            Log.v(TAG,"mode de repetition :"+repeatMod);
            try 
            {
                switch (repeatMod) 
                {
                case 0:
                    repeatMusic.setText(R.string.rep_no);
                    mService.setLooping(false);    
                    break;
                case 1:
                    repeatMusic.setText(R.string.rep_one);
                    mService.setLooping(true);
                    break;
                case 2:
                    repeatMusic.setText(R.string.rep_all);
                    mService.setRepeatAll();
                    break;
                default :
                    repeatMusic.setText(R.string.play);
                    mService.setLooping(false);
                    break;
                }
            } catch (DeadObjectException doe) 
            {
                Log.v(TAG,doe.toString());
            }
        }

        public void onServiceDisconnected(ComponentName className)
        {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
                        
        }
    };
    
    //  Fill the table Song with mp3 found in path
    private void fillBD (String path)
    {
        try{
            // ?? long t = System.currentTimeMillis();
            // get all mp3 files in path & insert them in the database
            for(String file : Directory.scanFiles(path, ".mp3")){ //ugly string .mp3
                mdb.insert(file);                
            }
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
        
        SharedPreferences settings = getSharedPreferences(PREF_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("repeatMod",repeatMod);

        editor.commit();
        unbindService(mConnection);
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
