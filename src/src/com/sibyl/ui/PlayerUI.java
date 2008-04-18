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
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteDiskIOException;
import android.graphics.drawable.Drawable;
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
import com.sibyl.ui.ProgressBarClickable.OnProgressChangeListener;


/**
 * The player activity. It launches the service and you can control the music: play, stop, play next/previous.
 * You have access to the other activities: options and playlist
 * @author Sibyl-dev
 */
public class PlayerUI extends Activity
{

    ISibylservice mService = null;
    //menu variables
    private static final int QUIT_ID = Menu.FIRST;
    private static final int PLAYLIST_ID = Menu.FIRST +1;
    private static final int OPTION_ID = Menu.FIRST +2;
    private static final int COVER_ID = Menu.FIRST +3;

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
    private ImageView cover;
    private ProgressView progress;

    private MusicDB mdb;    //the database
    private static String pathCover;

    //handler to call function when datas are received from the service
    private Handler mServHandler = new Handler();

    private Handler mProgHandler = new Handler();
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
            progress.setProgress(timer);
            elapsedTime.setText(DateUtils.formatElapsedTime(timer/1000));
            // again in 0.1s
            mTimeHandler.postDelayed(this, 1000);
            //progress.redraw();
        }
    };
    

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
        pathCover = "";
        lecture.requestFocus();
        //launch the service.
        launchService();
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
    
    /*
     * Initialize the progressbar: set the total time and elapsed time
     */
    private void initializeProgressTime(){
        //progress.initializeProgress();
        try{
            progress.setTotal(mService.getDuration());
            progress.setProgress(mService.getCurrentPosition());
        }
        catch( DeadObjectException ex){}
    }

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
       // avance.setEnabled(enable);
    }
    
    /**
     * gets views and initialize them
     */
    private void initializeViews(){        
        //get buttons
        lecture = (Button) findViewById(R.id.lecture);
        next = (Button) findViewById(R.id.next);
        previous = (Button) findViewById(R.id.prec);
        ///avance = (Button) findViewById(R.id.avance);
        
        //avance.setVisibility(4);
        
        //disable buttons until we are connected to the service
        enableButtons(false);
        
        //set listeners
        lecture.setOnClickListener(mPlayListener);
        next.setOnClickListener(mNextListener);
        previous.setOnClickListener(mPreviousListener);
        //avance.setOnClickListener(mAvanceListener);
        
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
        cover = (ImageView) findViewById(R.id.cover);
        cover.setImageResource(R.drawable.logo);
        
        //get progress
        progress = (ProgressView) findViewById(R.id.progress);
        progress.initializeProgress();
        progress.setOnProgressChangeListener(changeListener);
    }
    
    

    /*
     * Manage when the progressbar is clicked.
     * If a music was played or paused, the song is played at the new position
     * If the service was stopped, but the playlist filled, we launch the play
     */
    private OnProgressChangeListener changeListener = new OnProgressChangeListener() {
            public void onProgressChanged(View v, int progress) {
                try{
                    if(mService.getState() == Music.State.PLAYING ||
                            mService.getState() == Music.State.PAUSED ||
                            (mService.getState() == Music.State.STOPPED && mdb.getPlaylistSize() > 0)){
                        mService.start();
                        mService.setCurrentPosition(progress);
                        //remove timer
                        mTimeHandler.removeCallbacks(timerTask);
                        // add timer task to ui thread
                        mTimeHandler.post(timerTask);
                    }
                    else{
                        PlayerUI.this.progress.setProgress(0);
                    }
                }
                catch(DeadObjectException ex){}
            }
    };
    
    
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
    }

    @Override
    protected void onResume() 
    {
        super.onResume();
        if( mService != null){
            updateUI();
            try{
                if(mService.getState() == Music.State.PLAYING ||
                        mService.getState() == Music.State.PAUSED){
                    initializeProgressTime();
                }
            }
            catch( DeadObjectException ex){}
        }
    }
    
    /*
     * launch the service
     */
    public void launchService()    
    {
        bindService(new Intent(PlayerUI.this,
            Sibylservice.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        super.onCreateOptionsMenu(menu);
        menu.add(0, QUIT_ID, R.string.menu_quit);
        menu.add(0, PLAYLIST_ID, R.string.menu_playList);
        menu.add(0, OPTION_ID, R.string.menu_option);
        menu.add(0, COVER_ID, "CoverManage");
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
            case COVER_ID:
                Intent i = new Intent(this, AlbumUI.class);
                startSubActivity(i, 0);
                break;
        }
    
        return true;
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
    
            //DEBUG remplacant du NotificationManager/notifyWithText
            Toast.makeText(PlayerUI.this, "Connexion au service reussie", 
                Toast.LENGTH_SHORT).show();
    
            //connection of the service to the activity
            try {
                mService.connectToReceiver(mServiceListener);
                //now that we are connected to the service, user can click on buttons
                //to start playing music
                enableButtons(true);
                progress.initializeProgress();
                updateUI();
            }
            catch(DeadObjectException ex){
            Log.v(TAG, ex.toString());
            // user should be warned
            }
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


    /*
     * set the total time of the song which is played
     */
    private void setTotalTime ()
    {
        try 
        {
            tempsTotal.setText(DateUtils.formatElapsedTime(mService.getDuration()/1000));
        }
        catch (DeadObjectException ex){
            Log.v(TAG, ex.toString());
            // user should be warned or maybe it is not that important
        }
    }



    /*
     * listener for the button Play/Pause. It stops the reading if the service is stopped or start/resume it.
     */
    private OnClickListener mPlayListener = new OnClickListener()
    {
    public void onClick(View v)
    {
        try{ 
                if( mService.getState() == Music.State.PLAYING) //call if a music is played (pause the music)
                {
                    mService.pause();
                    lecture.setText(R.string.play);
                    // remove timer task from ui thread
                    mTimeHandler.removeCallbacks(timerTask);
                }
                else // to start listening a music or resume.
                {
                    mService.start();
                    if( mService.getState() != Music.State.ERROR){
                        lecture.setText(R.string.pause);
                        //updateUI(); //display informations about the song
                        mTimeHandler.post(timerTask);
                    }
                    else{
                        noSongToPlay();
                    }
                }
        }
        catch(DeadObjectException ex) {}
        }     
    };


    /*
     * Listenner for the Button Next. Play the next song in the playlist
     */
    private OnClickListener mNextListener = new OnClickListener()
    {
        public void onClick(View v)
        {
            playSong(PLAY.NEXT);
        }
    };

    /*
     * Listenner for the button previous. Play the previous sont in the playlist
     */
    private OnClickListener mPreviousListener = new OnClickListener()
    {
        public void onClick(View v)
        {
            playSong(PLAY.PREV);
        }
    };


    //Listener for the Button Avance. Avance the lecture of the song of 30sec
    //useful for tests of handling the end of the song
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
        mTimeHandler.removeCallbacks(timerTask);
        mTimeHandler.post(timerTask);
        }
        catch (DeadObjectException ex){
        Log.v(TAG, ex.toString());
        // user should be warned
        }
    }
    };

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

    /*
     * Manage the display of the UI. Displays artist and song's name, total time, and launch the timer when a song is played
     * If nothing is played (end of playlist or playlist empty), it shows: Sibyl, mobile your music
     * disable the button when the end of playlist is reached
     */
    private void updateUI() 
    {
        try{
            int state = mService.getState();
            if( state == Music.State.PLAYING 
                    || state == Music.State.PAUSED
                    || state == Music.State.STOPPED){
                //reset the timer's time
                //display the song and artist name
                int pos=mService.getCurrentSongIndex();
                Log.v(TAG, "updateUI: pos="+pos);
                // is there a song to display ?
                if(pos > 0 && pos <= mdb.getPlaylistSize() ){
                    setTotalTime();
                    String [] songInfo = mdb.getSongInfoFromCP(pos);
                    titre.setText(songInfo[0]);
                    artiste.setText(songInfo[1]);
                    //if the song has a cover and it's not the cover which is displayed
                    if( songInfo[2] != null && !songInfo[2].equals(pathCover)){
                        pathCover=songInfo[2];
                        cover.setImageDrawable(Drawable.createFromPath(pathCover));
                    }
                    else if(songInfo[2] == null){ //displayed default logo
                        cover.setImageResource(R.drawable.logo);
                        pathCover = null;
                    }

                    if( mService.getState() == Music.State.PLAYING) {
                        //remove timer
                        mTimeHandler.removeCallbacks(timerTask);
                        // add timer task to ui thread
                        mTimeHandler.post(timerTask);
                        lecture.setText(R.string.pause);
                    }             
                    else {
                        lecture.setText(R.string.play);
                    }
                }else{
                    noSongToPlay();
                }
                enableButtons(true);
            }
            else if(state == Music.State.END_PLAYLIST_REACHED){
                noSongToPlay();
                enableButtons(false);
            }
            
        }
        catch( DeadObjectException ex){} 
    }

    /* TODO Supprimer la communication avec le service quand la fenetre n'est plus affichée ? */
    //communication from the service
    private final IPlayerUI.Stub mServiceListener = new IPlayerUI.Stub() 
    {
        
        public void handleStartPlaying() 
        {
            Log.v(TAG, "Starting playing a song handled in PlayerUI");
            mServHandler.post(new Runnable()
            {
            public void run()
            {
                updateUI();
                initializeProgressTime();
            }
        });
    
        } 
        
        public void handleEndSong() 
        {
            Log.v(TAG, "End song handled in PlayerUI");
            mServHandler.post(new Runnable()
            {
            public void run()
            {
                updateUI();
            }
            });
        
        }   

        public void handleEndPlaylist()
        {
            mServHandler.post(new Runnable()
            {
            public void run()
            {
                updateUI();
                enableButtons(false);
                progress.setProgress(0);
            }
            });
        }
    };


    /*
     * When nothing is played; it's the default display 
     */
    private void noSongToPlay()
    {
        mTimeHandler.removeCallbacks(timerTask);
        elapsedTime.setText(DateUtils.formatElapsedTime(0));
        artiste.setText(R.string.artiste);
        tempsTotal.setText(R.string.time_zero);
        titre.setText(R.string.titre);
        lecture.setText(R.string.play);
        //previous.setEnabled(false);
    }


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
                playSong(PLAY.PREV);
                previous.setBackground(android.R.drawable.btn_default);
            }
            //lecture.requestFocus();
            return true;
        case KeyEvent.KEYCODE_DPAD_RIGHT :
            if( next.isEnabled()){
                playSong(PLAY.NEXT);
                next.setBackground(android.R.drawable.btn_default);
            }
            //lecture.requestFocus();
            return true;
        case KeyEvent.KEYCODE_DPAD_CENTER :
            //lecture.requestFocus();
            if( lecture.isEnabled()){
                lecture.performClick();
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

    /*call the service methods prev() or next() in function of type and refresh the UI
     * @param type indicates wich buttons: next or previous has been clicked
    */
    private void playSong( int type){
    try
    {
        if( type == PLAY.NEXT) {
            mService.next();
        }
        else{
            mService.prev();
        }
        updateUI(); //refresh the display

    }
        catch (DeadObjectException ex) {
            Log.v(TAG, ex.toString());
            // user warned ?
    }
    }

}
