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
import java.util.HashMap;
import java.util.Map;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDiskIOException;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.ViewFlipper;
import android.widget.AdapterView.OnItemSelectedListener;

import com.sibyl.ISibylservice;
import com.sibyl.Music;
import com.sibyl.MusicDB;
import com.sibyl.R;
import com.sibyl.Sibylservice;

public class PlayListUI extends ListActivity
{
    private static final int BACK_ID = Menu.FIRST;
    private static final int ADD_ID = Menu.FIRST +1;
    private static final int NEW_ID = Menu.FIRST +2;

    private static final String TAG = "PLAYLIST";
    private static final int ADD_UI = 1;

    private Animation outAnimation;
    private Animation inAnimation;

    ISibylservice mService = null;

    private int songPlayed;

    //the database
    private MusicDB mdb;    

    // Animation task
    private Runnable animTask = new Runnable(){
        public void run(){
            // start flipping if selected item is selected
            if( getListView().getSelectedView() != null && 
                    (ViewFlipper)(getListView().getSelectedView().findViewById(R.id.text_switcher)) == previous){
                // called twice so we avoid second call
                previous.setInAnimation(inAnimation);
                previous.setOutAnimation(outAnimation);
                previous.startFlipping();
            }
        }
    };
    // the last row where we started an animation
    private ViewFlipper previous; 


    private IntentFilter intentF;
    private BroadcastReceiver intentHandler = new BroadcastReceiver(){
        public void onReceive(Context c, Intent i){
            // we are interessed if a new song is played
            if(i.getAction().equals(Music.Action.PLAY)){
                fillData();
            }else if(i.getAction().equals(Music.Action.NEXT)){
                fillData();
            }else if(i.getAction().equals(Music.Action.PREVIOUS)){
                fillData();
            }else if(i.getAction().equals(Music.Action.NO_SONG)){
                fillData();
            }
        }
    };

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

    /* ----------------------- ACTIVITY STATES -------------------------------*/
    private Handler mHandler = new Handler();

    /** 
     * Called when the activity is first created. 
     */
    public void onCreate(Bundle icicle) 
    {
        super.onCreate(icicle);
        setTitle(R.string.playlist_title);
        //register intent so we will be aware of service changes
        intentF = new IntentFilter();
        intentF.addAction(Music.Action.PLAY);
        intentF.addAction(Music.Action.NO_SONG);
        intentF.addAction(Music.Action.NEXT);
        intentF.addAction(Music.Action.PREVIOUS);
        launchService();
        songPlayed = 0;
        setContentView(R.layout.playlist);

        inAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        outAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);

        // to manage animation when moving in the list
        getListView().setOnItemSelectedListener(new OnItemSelectedListener(){
            public void onItemSelected(AdapterView av, View v, int position, long id){
                ViewFlipper vs = (ViewFlipper)(v.findViewById(R.id.text_switcher));
                if(!vs.isFlipping()){
                    // stop previous viewflipper and display to song name
                    if(previous != null && previous.isFlipping()){
                        previous.stopFlipping();
                        if(previous.getDisplayedChild() != 0 ){
                            previous.showNext();
                        }
                    }
                    // set previous viewflipper
                    previous = vs;
                    // remove previous call to effect
                    mHandler.removeCallbacks(animTask);
                    // start flipping in 500ms
                    mHandler.postDelayed(animTask,500);
                }
            }
            public void onNothingSelected(AdapterView parent)
            {
                /* nothing to do */
            }
        });

        try
        {
            mdb = new MusicDB(this);
            //fillData();
        }
        catch(SQLiteDiskIOException ex)
        {
            //Log.v(TAG, ex.toString());
        }   
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // TODO Auto-generated method stub
        //Log.v(TAG, "event key up");
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // register intent handler, so we will be aware of service changes
        
        registerReceiver(intentHandler, intentF);
        if( mService != null){
            fillData();
        }
    }

    /**
     * when activity isn't displayed anymore
     */
    protected void onPause() 
    {
        // unregister intents
        unregisterReceiver(intentHandler);
        super.onPause();
    }

    protected void onDestroy() 
    {
        unbindService(mConnection);
        mdb.close();
        super.onDestroy();
    }

    /**
     * we have to know if songs have been added to playlist
     */
    protected void onActivityResult(int req, int res, String data, Bundle extras){
        // TODO mieux de le faire à la fin de l'activité
        // activty addUi, res = 1 -> changes
        if(req == ADD_UI && res == 1){
            try{
                mService.playlistChange();
            }
            catch(RemoteException ex){
                //Log.v(TAG, ex.toString());
            }
        }

    }

    /* ----------------------END ACTIVITY STATES -----------------------------*/

    /* -------------------------- utils---------------------------------------*/

    private void fillData()
    {
        Cursor c = mdb.getPlayListInfo();
        int icon;
        startManagingCursor(c);
        String[] colName = {"iconPl","text1","text2"};
        int[] to = {R.id.iconPl,R.id.text1, R.id.text2};

        ArrayList<Map<String, String>> rows = new ArrayList<Map<String, String>>();
        songPlayed = 0;
        try
        {
            int index = mService.getCurrentSongIndex()-1;
            int playerState = mService.getState();
            while(c.moveToNext())
            {
                // display icon if is playing and this song is played
                if( ( playerState == Music.State.PAUSED || playerState == Music.State.PLAYING )
                        && mService!= null && index == c.getPosition()){
                    icon = R.drawable.play_white;
                    songPlayed = c.getPosition();
                }
                else{
                    icon = R.drawable.puce;
                }
                Map<String, String> curMap = new HashMap<String, String>();
                rows.add(curMap);
                curMap.put(colName[0], ""+icon);
                curMap.put(colName[1], c.getString(0));
                curMap.put(colName[2], c.getString(1));
            }
        }
        catch(RemoteException ex){
            //Log.v(TAG, ex.toString());
        }
        
        SimpleAdapter adapter = new SimpleAdapter(this.getApplicationContext(), rows,
                R.layout.playlist_row,colName,to);
        stopManagingCursor(c);
        c.close();
        setListAdapter(adapter);
        setSelection(songPlayed);
    }

    private void launchService() 
    {
        bindService(new Intent(PlayListUI.this,
                Sibylservice.class), mConnection, Context.BIND_AUTO_CREATE);
    }
    /* ---------------------  END utils --------------------------------------*/

    /* ---------------------  UI menu ----------------------------------------*/

    public boolean onCreateOptionsMenu(Menu menu) 
    {
        super.onCreateOptionsMenu(menu);
        menu.add(0, BACK_ID, Menu.NONE, R.string.menu_back);
        menu.add(0, ADD_ID, Menu.NONE, R.string.menu_add);
        menu.add(0, NEW_ID, Menu.NONE, R.string.menu_new);
        return true;
    }

    /*when a menu is selected*/
    public boolean onMenuItemSelected(int featureId, MenuItem item) 
    {
        super.onMenuItemSelected(featureId, item);
        Intent i = null;
        switch(item.getItemId()) 
        {
            case ADD_ID:
                i = new Intent(this, AddUI.class);
                startActivityForResult(i, ADD_UI);
                break;
            case NEW_ID:
                try 
                {
                    mService.clear();
                } catch (RemoteException e) {
                    //Log.v(TAG, e.toString());
                    // warn user
                }
                i = new Intent(this, AddUI.class);
                startActivityForResult(i, ADD_UI);
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
        } catch (RemoteException e) {
            //Log.v(TAG, e.toString());
        }
    }

}
