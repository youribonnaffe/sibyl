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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentReceiver;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteDiskIOException;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.util.DateUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu.Item;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.sibyl.ISibylservice;
import com.sibyl.Music;
import com.sibyl.MusicDB;
import com.sibyl.R;
import com.sibyl.Sibylservice;


/**
 * The player activity. It launches the service and you can control the music: play, stop, play next/previous.
 * You have access to the other activities: options et playlist
 * @author Sibyl-dev
 */
public class PlayerUI extends Activity
{

    ISibylservice mService = null;
    //menu variables
    private static final int QUIT_ID = Menu.FIRST;
    private static final int PLAYLIST_ID = Menu.FIRST +1;
    private static final int OPTION_ID = Menu.FIRST +2;

    public static class PLAY {
        public static int NEXT = 0;
        public static int PREV = 1;
    }

    private static final String TAG = "COLLECTION";

    //views of the ui
    private TextView artiste;
    private TextView titre;
    private TextView elapsedTime;
    private TextView tempsTotal;
    private Button lecture;
    private Button next;
    private Button previous;
    private Button avance;

    private MusicDB mdb;    //the database
    
    IntentFilter intentF;

    //handler for calculating elapsed time when playing a song
    private Handler mTimeHandler = new Handler();
    //thread which shows the elapsed time when a song is played.
    private Runnable timerTask = new Runnable() 
    {
        private int timer = 0;
        public void run() 
        {
            // re adjusting time
            try
            {
                // get time
                if(mService.getState() == Music.State.PLAYING){
                    timer = mService.getCurrentPosition(); // seems to avoid ANR
                }
            }
            catch(DeadObjectException ex){
                // old value will be kept, if the song can't be played anymore
                // an error should be thrown by the service (or not)
            }
            elapsedTime.setText(DateUtils.formatElapsedTime(timer/1000));
            // again in 0.1s
            mTimeHandler.postDelayed(this, 1000);
        }
    };

    private IntentReceiver intentHandler = new IntentReceiver(){
        public void onReceiveIntent(Context c, Intent i){
            Log.v("INTENT", "PLAYERUI RECEIVE "+i.toString());

            // call appropriate refresh methods
            if(i.getAction().equals(Music.Action.PLAY)){
                playRefresh();
            }else if(i.getAction().equals(Music.Action.PAUSE)){
                pauseRefresh();
            }else if(i.getAction().equals(Music.Action.NEXT)){
                nextRefresh();
            }else if(i.getAction().equals(Music.Action.PREVIOUS)){
                previousRefresh();
            }else if(i.getAction().equals(Music.Action.NO_SONG)){
                noSongRefresh();
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

            //DEBUG remplacant du NotificationManager/notifyWithText
            Toast.makeText(PlayerUI.this, "Connexion au service reussie", 
                    Toast.LENGTH_SHORT).show();

            //now that we are connected to the service, user can click on buttons
            //to start playing music
            enableButtons(true);
            //updateUI();
            // TODO ???
            pauseRefresh();
            songRefresh();

        }

        public void onServiceDisconnected(ComponentName className)
        {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            // IMPORTANT : should freeze the application and user has to relaunch it

            //remplacant du NotificationManager/notifyWithText
            Toast.makeText(PlayerUI.this, "Deconnexion du service", 
                    Toast.LENGTH_SHORT).show(); 

            //as we are disconnected from the service, user can't play music anymore
            //so we disable the buttons
            enableButtons(false);
            // updateUI(); ??
        }
    };
    
    /* ----------------------- ACTIVITY STATES -------------------------------*/

    /** 
     * Called when the activity is first created. It initialize the ui: buttons, labels.
     * Launches the service.
     * connect the ui to the database
     */
    @Override
    public void onCreate(Bundle icicle) 
    {
        super.onCreate(icicle);
        setTitle(R.string.app_name);
        setContentView(R.layout.main);
        initializeViews();
        lecture.requestFocus();
        //launch the service.
        launchService();
        
        //register intent so we will be aware of service changes
        intentF = new IntentFilter();
        intentF.addAction(Music.Action.PAUSE);
        intentF.addAction(Music.Action.PLAY);
        intentF.addAction(Music.Action.PREVIOUS);
        intentF.addAction(Music.Action.NEXT);
        intentF.addAction(Music.Action.NO_SONG);
        //create or connect to the Database
        try
        {
            mdb = new MusicDB(this);
        }
        catch(SQLiteDiskIOException e)
        {
            Log.v(TAG, e.getMessage());
            // user sould be warned
        }
    }

    @Override
    protected void onDestroy() 
    {
        super.onDestroy();
        unbindService(mConnection);
    }

    @Override
    protected void onStop() 
    {
        super.onStop();
        mTimeHandler.removeCallbacks(timerTask); // stop the timer update
        // unregister intents
        unregisterReceiver(intentHandler);
    }

    @Override
    protected void onResume() 
    {
        super.onResume();

        // when displayed we want to be informed of service changes
        registerReceiver(intentHandler, intentF);

        // refresh ui when displaying the activity if service connection already made
        // TODO maybe 2 calls
        if( mService != null){
            songRefresh();
            timerRefresh();
        }
    }

    /* ----------------------END ACTIVITY STATES -----------------------------*/

    /* -------------------------- utils---------------------------------------*/

    /**
     * Enables or disables the buttons
     *
     * @param enable     if true, enables the buttons, else disables them
     */
    private void enableButtons(boolean enable)
    {
        lecture.setEnabled(enable);
        next.setEnabled(enable);
        previous.setEnabled(enable);
        avance.setEnabled(enable);
    }

    /*
     *launch the activity PlayerListUI: show and manage the playlist 
     */
    private void displayPlaylist() 
    {
        Intent i = new Intent(this, PlayListUI.class);
        startSubActivity(i, 0);
    }

    /*
     * launch the activity ConfigUI.
     */
    private void displayConfig() 
    {
        Intent i = new Intent(this, ConfigUI.class);
        startSubActivity(i, 0);
    }

    /**
     * gets views and initialize them
     */
    private void initializeViews(){        
        //get buttons
        lecture = (Button) findViewById(R.id.lecture);
        next = (Button) findViewById(R.id.next);
        previous = (Button) findViewById(R.id.prec);
        avance = (Button) findViewById(R.id.avance);

        // avance.setVisibility(4);

        // disable buttons until we are connected to the service
        enableButtons(false);

        //set listeners
        lecture.setOnClickListener(mPlayListener);
        next.setOnClickListener(mNextListener);
        previous.setOnClickListener(mPreviousListener);
        avance.setOnClickListener(mAvanceListener);

        //set focusable
        next.setFocusableInTouchMode(true);
        previous.setFocusableInTouchMode(true);
        lecture.setFocusableInTouchMode(true);

        //get labels
        artiste = (TextView) findViewById(R.id.artiste);
        titre = (TextView) findViewById(R.id.titre);
        tempsTotal = (TextView) findViewById(R.id.tpsTotal);
        elapsedTime = (TextView) findViewById(R.id.tpsEcoule);

        elapsedTime.setText(DateUtils.formatElapsedTime(0));
        tempsTotal.setText(DateUtils.formatElapsedTime(0));

        //set cover
        ImageView cover = (ImageView) findViewById(R.id.cover);
        cover.setImageResource(R.drawable.logo);  
    }

    /*
     * launch the service
     */
    private void launchService()    
    {
        bindService(new Intent(PlayerUI.this,
                Sibylservice.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    /* ---------------------  END utils --------------------------------------*/

    /* ---------------------  UI menu ----------------------------------------*/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        super.onCreateOptionsMenu(menu);
        menu.add(0, QUIT_ID, R.string.menu_quit);
        menu.add(0, PLAYLIST_ID, R.string.menu_playList);
        menu.add(0, OPTION_ID, R.string.menu_option);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, Item item) 
    {
        super.onMenuItemSelected(featureId, item);
        switch(item.getId()) 
        {
            case QUIT_ID:
                try 
                {
                    mService.stop();
                }
                catch (DeadObjectException ex) {
                    Log.v(TAG, ex.toString());
                    // user should be warned
                }
                lecture.setText(R.string.play);
                finish();
                break;
            case PLAYLIST_ID:
                //launch the playlist's activity
                displayPlaylist();
                break;
            case OPTION_ID:
                //launch the option's activity
                displayConfig();
                break;
        }

        return true;
    }
    /* --------------------- END UI menu -------------------------------------*/

    /* ---------------------  UI actions listener ----------------------------*/

    public boolean onTouchEvent(MotionEvent ev){
        //Log.v("touche event", ev.toString()+ " focus : "+ this.getCurrentFocus());
        lecture.requestFocus();
        //Log.v("focus", "o"+this.getCurrentFocus());
        return super.onTouchEvent(ev);
    }

    public boolean onKeyUp(int keycode, KeyEvent event){
        //Log.v("event up", event.toString());
        switch(event.getKeyCode()){
            case KeyEvent.KEYCODE_DPAD_DOWN :
                return true;
            case KeyEvent.KEYCODE_DPAD_UP :
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT :
                if(previous.isEnabled()){
                    try{
                        mService.prev();
                        previous.setBackground(android.R.drawable.btn_default);
                    }catch(DeadObjectException doe){
                        Log.v(TAG, doe.toString());
                    }
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT :
                if( next.isEnabled()){
                    try{
                        mService.next();
                        next.setBackground(android.R.drawable.btn_default);
                    }catch(DeadObjectException doe){
                        Log.v(TAG, doe.toString());
                    }
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER :
                if( lecture.isEnabled()){
                    playPauseAction();
                }
                return true;
        }
        return super.onKeyUp(keycode, event);
    }

    public boolean onKeyDown(int keycode, KeyEvent event){
        //Log.v("event down", event.toString());
        switch(keycode){
            case KeyEvent.KEYCODE_DPAD_DOWN :
                return true;
            case KeyEvent.KEYCODE_DPAD_UP :
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT :
                if( previous.isEnabled()){
                    previous.setBackground(android.R.drawable.btn_default_selected);
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT :
                if( next.isEnabled()){
                    next.setBackground(android.R.drawable.btn_default_selected);
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER :
                return true;
        }
        return super.onKeyDown(keycode, event);
    }


    /*
     * listener for the button Play/Pause. It stops the reading if the service is stopped or start/resume it.
     */
    private OnClickListener mPlayListener = new OnClickListener()
    {
        public void onClick(View v)
        {
            playPauseAction();
        }
    };

    /*
     * Listenner for the Button Next. Play the next song in the playlist
     */
    private OnClickListener mNextListener = new OnClickListener()
    {
        public void onClick(View v)
        {
            try{
                mService.next();
            }catch(DeadObjectException doe){
                Log.v(TAG, doe.toString());
            }
        }
    };

    /*
     * Listenner for the button previous. Play the previous sont in the playlist
     */
    private OnClickListener mPreviousListener = new OnClickListener()
    {
        public void onClick(View v)
        {
            try{
                mService.prev();
            }catch(DeadObjectException doe){
                Log.v(TAG, doe.toString());
            }
        }
    };


    //  Listener for the Button Avance. Avance the lecture of the song of 30sec
    //  useful for tests of handling the end of the song
    private OnClickListener mAvanceListener = new OnClickListener()
    {
        public void onClick(View v)
        {
            try
            {
                //avance de 30sec (30000ms)
                int newTime=mService.getCurrentPosition()+30000;
                if(newTime >= mService.getDuration()) 
                {
                    newTime=mService.getDuration()-3000;
                }
                mService.setCurrentPosition(newTime);

                //update of the current time displayed
                // ugly but only for debug
                playRefresh();
            }
            catch (DeadObjectException ex){
                Log.v(TAG, ex.toString());
                // user should be warned
            }
        }
    };
    
    /* --------------------- END UI actions listener -------------------------*/

    private void playPauseAction (){
        try{ 
            if( mService.getState() == Music.State.PLAYING) //call if a music is played (pause the music)
            {
                mService.pause();
            }
            else // to start listening a music or resume.
            {
                mService.start();
            }
        }
        catch(DeadObjectException ex) {
            Log.v(TAG,ex.toString());
        }
    }

    private void timerRefresh(){
        try{
            mTimeHandler.removeCallbacks(timerTask);
            if(mService.getState() == Music.State.PLAYING){
                mTimeHandler.post(timerTask);
            }else{
                elapsedTime.setText(DateUtils.formatElapsedTime(mService.getDuration()/1000));
            }
        }catch( DeadObjectException doe){
            Log.v(TAG, doe.toString());
        }
    }


    private void playRefresh(){
        lecture.setText(R.string.pause);
        //update of the current time displayed
        mTimeHandler.removeCallbacks(timerTask);
        mTimeHandler.post(timerTask);
    }

    private void pauseRefresh(){
        lecture.setText(R.string.play);
        // stop timer update
        mTimeHandler.removeCallbacks(timerTask);
    }

    private void songRefresh(){
        try{
            // refresh total time, song infos
            int pos=mService.getCurrentSongIndex();
            int plSize = mdb.getPlaylistSize();
            // set total time
            tempsTotal.setText(DateUtils.formatElapsedTime(mService.getDuration()/1000));
            // ensure that playlist isn't empty
            if (pos >= 1 && pos <= plSize){
                String [] songInfo = mdb.getSongInfoFromCP(pos);
                titre.setText(songInfo[0]);
                artiste.setText(songInfo[1]);
            }
            // buttons next & prev update
            if(pos == 1){
                // disable previous button
                previous.setEnabled(false);
            }else{
                previous.setEnabled(true);
            }
            
            if(mdb.getPlaylistSize() <= 0 || pos == mdb.getPlaylistSize()){
                next.setEnabled(false);
            }else{
                next.setEnabled(true);
            }
            
        }catch( DeadObjectException doe){
            Log.v(TAG, doe.toString());
        }
    }

    private void nextRefresh(){
        songRefresh();
        // TODO maybe we should disable next button if it is the last song
        //.?? TODO
        enableButtons(true);
    }

    private void previousRefresh(){
        songRefresh();
        //same thing for now
    }

    private void noSongRefresh(){
        // remove timer
        mTimeHandler.removeCallbacks(timerTask);
        // reset all 
        elapsedTime.setText(DateUtils.formatElapsedTime(0));
        artiste.setText(R.string.artiste);
        tempsTotal.setText(R.string.time_zero);
        titre.setText(R.string.titre);
        lecture.setText(R.string.play);

        // TODO
        enableButtons(false);

    }
}