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

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentReceiver;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDiskIOException;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Menu.Item;
import android.widget.ListView;

import com.sibyl.ISibylservice;
import com.sibyl.Music;
import com.sibyl.MusicDB;
import com.sibyl.R;
import com.sibyl.Sibylservice;

public class PlayListUI extends ListActivity
{
    private static final int ADD_ID = Menu.FIRST;
    private static final int NEW_ID = Menu.FIRST +1;
    private static final int BACK_ID = Menu.FIRST +2;

    private static final String TAG = "PLAYLIST";

    ISibylservice mService = null;

    private int songPlayed;

    //the database
    private MusicDB mdb;    

    private IntentFilter intentF;
    private IntentReceiver intentHandler = new IntentReceiver(){
        public void onReceiveIntent(Context c, Intent i){
            //Log.v("INTENT", "RECEIVE PLAYLIST "+i.toString());
            // we are interessed if a new song is played
            if(i.getAction().equals(Music.Action.PLAY)){
                fillData();
            }else if(i.getAction().equals(Music.Action.NO_SONG)){
                fillData();
            }
        }
    };

    /** 
     * Called when the activity is first created. 
     */
    public void onCreate(Bundle icicle) 
    {
        super.onCreate(icicle);
        //register intent so we will be aware of service changes
        intentF = new IntentFilter();
        intentF.addAction(Music.Action.PLAY);
        intentF.addAction(Music.Action.NO_SONG);
        launchService();
        songPlayed = 0;
        setContentView(R.layout.playlist);
        try
        {
            mdb = new MusicDB(this);
            //fillData();
        }
        catch(SQLiteDiskIOException ex)
        {
            Log.v(TAG, ex.toString());
        }   
    }

    /**
     * when activity isn't displayed anymore
     */
    protected void onPause() 
    {
        super.onPause();
        // unregister intents
        unregisterReceiver(intentHandler);
    }

    protected void onResume() {
        // register intent handler, so we will be aware of service changes
        registerReceiver(intentHandler, intentF);

        if( mService != null){
            fillData();
        }
        super.onResume();
    }

    protected void onDestroy() 
    {
        super.onDestroy();
        unbindService(mConnection);        
    }

    public boolean onCreateOptionsMenu(Menu menu) 
    {
        super.onCreateOptionsMenu(menu);
        menu.add(0, ADD_ID, R.string.menu_add);
        menu.add(0, NEW_ID, R.string.menu_new);
        menu.add(0, BACK_ID, R.string.menu_back);
        return true;
    }

    /*when a menu is selected*/
    public boolean onMenuItemSelected(int featureId, Item item) 
    {
        super.onMenuItemSelected(featureId, item);
        Intent i = null;
        switch(item.getId()) 
        {
            case ADD_ID:
                i = new Intent(this, AddUI.class);
                startSubActivity(i, 0);
                break;
            case NEW_ID:
                try 
                {
                    mService.clear();
                } catch (DeadObjectException e) {
                    Log.v(TAG, e.toString());
                    // warn user
                }
                i = new Intent(this, AddUI.class);
                startSubActivity(i, 0);
                break;
            case BACK_ID:
                finish();
                break;
        }
        return true;
    }

    protected void onListItemClick(ListView l, View v, int position, long id) 
    {
        position ++;
        try {
            mService.playSongPlaylist(position);
        } catch (DeadObjectException e) {
            Log.v(TAG, e.toString());
        }
    }

    private void fillData()
    {
        Cursor c = mdb.getPlayListInfo();
        int icon;
        startManagingCursor(c);
        IconifiedTextListAdapter rows = new IconifiedTextListAdapter(this);
        songPlayed = 0;
        try{
            int index = mService.getCurrentSongIndex()-1;
            int playerState = mService.getState();
            while(c.next()){
                // display icon if is playing and this song is played
                if( playerState == Music.State.PLAYING && mService!= null && index == c.position()){
                    icon = R.drawable.play_white;
                    songPlayed = c.position();
                }
                else{
                    icon = R.drawable.puce;
                }
                rows.add(new IconifiedText( c.getString(0)+ getString(R.string.sep_artiste_song)+  c.getString(1),
                        getResources().getDrawable(icon)));
            }
        }
        catch(DeadObjectException ex){
            Log.v(TAG, ex.toString());
        }
        c.close();        
        setListAdapter(rows);
        setSelection(songPlayed);
    }

    private void launchService() 
    {
        bindService(new Intent(PlayListUI.this,
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
            /* Positionner le selecteur de la liste sur la chanson en cours
             * une fois que le service est connecté
             * */
            fillData();
        }

        public void onServiceDisconnected(ComponentName className)
        {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;      
        }
    };
}
