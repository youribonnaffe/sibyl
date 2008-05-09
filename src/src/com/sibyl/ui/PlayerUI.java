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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu.Item;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.sibyl.ISibylservice;
import com.sibyl.Music;
import com.sibyl.MusicDB;
import com.sibyl.R;
import com.sibyl.Sibylservice;
import com.sibyl.ui.AnimatedCover.Move;
import com.sibyl.ui.ProgressBarClickable.OnProgressChangeListener;

/**
 * The player activity. It launches the service and you can control the music: play, stop, play next/previous.
 * You have access to the other activities: options and playlist
 * @author Sibyl-dev
 */ 
public class PlayerUI extends Activity
{

    // menu variables
    private static final int PLAYLIST_ID = Menu.FIRST ;
    private static final int OPTION_ID = Menu.FIRST +1;
    private static final int QUIT_ID = Menu.FIRST +2;
    private static final int COVER_ID = Menu.FIRST +3;

    // debug
    private static final String TAG = "PLAYERUI";

    // views of the ui
    private TextView artiste;
    private TextView titre;
    private Button lecture;
    private Button next;
    private Button previous;
    private AnimatedCover cover;
    private ProgressView progress;
    private ImageView imageOver;
    
    private AlphaAnimation imageOverAnim;

    private MusicDB mdb;    //the database
    private ISibylservice mService = null;
    private IntentFilter intentF;
    private static String pathCover;
    
    //Click information for the touch mode
    private float firstClickPosX;
    private float firstClickPosY;
    private float firstClickTime;
    //constants for touch mode
    private static final float gapDbClickTime = 500;
    private static final float gapTime = 1000;
    private static final float gapSmall = 60;
    private static final float gapLong = 100;
    private static final float gapDbClick = 50;

    private int maxTimer;
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
            if(timer <= maxTimer){
               progress.setProgress(timer);
            }else{
                progress.setProgress(maxTimer);
            }
            // again in 1s
            mTimeHandler.postDelayed(this, 1000);
        }
    };

    private IntentReceiver intentHandler = new IntentReceiver(){
        public void onReceiveIntent(Context c, Intent i){
            Log.v("INTENT", "PLAYERUI RECEIVED "+i.toString());
            // call appropriate refresh methods
            if(i.getAction().equals(Music.Action.PLAY)){
                playRefresh();
            }else if(i.getAction().equals(Music.Action.PAUSE)){
                pauseRefresh();
            }else if(i.getAction().equals(Music.Action.NEXT)){
                songRefresh(Move.NEXT);
            }else if(i.getAction().equals(Music.Action.PREVIOUS)){
                songRefresh(Move.PREV);
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
            //Toast.makeText(PlayerUI.this, "Connexion au service reussie", 
              //      Toast.LENGTH_SHORT).show();

            //now that we are connected to the service, user can click on buttons
            //to start playing music
            resumeRefresh();
            // TODO put boolean to avoid too calls
        }

        public void onServiceDisconnected(ComponentName className)
        {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // mService = null; not sure that it is a good idea since it will crash everything
            // IMPORTANT : should freeze the application and user has to relaunch it

            //remplacant du NotificationManager/notifyWithText
            //Toast.makeText(PlayerUI.this, "Deconnexion du service", 
              //      Toast.LENGTH_SHORT).show(); 

            //as we are disconnected from the service, user can't play music anymore
            //so we disable the buttons
            enableButtons(false);
            // TODO freeze total ?
            // remove calls to timer
            mTimeHandler.removeCallbacks(timerTask);
        }
    };

    /* ----------------------- ACTIVITY STATES -------------------------------*/

    /** 
     * Called when the activity is first created. It initializes the ui: buttons, labels.
     * Launches the service.
     * Connect the ui to the database
     */
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
            // user should be warned
        }
        
        //animation for imageOver which disappears smoothly with an alpha animation
        imageOverAnim = new AlphaAnimation(1.0f, 0.0f);
        imageOverAnim.setStartOffset(1000);
        imageOverAnim.setDuration(800);
        imageOverAnim.setInterpolator(new LinearInterpolator());
    }

    protected void onDestroy() 
    {
        unbindService(mConnection);
        super.onDestroy();
    }

    /**
     * when activity isn't displayed anymore
     */
    protected void onPause() 
    {
        mTimeHandler.removeCallbacks(timerTask); // stop the timer update
        // unregister intents
        unregisterReceiver(intentHandler);
        super.onPause();
    }

    /**
     * when activity is displayed
     */
    protected void onResume() 
    {
        super.onResume();

        // when displayed we want to be informed of service changes
        registerReceiver(intentHandler, intentF);
        Log.v(TAG, "resume");
        if(mService != null){
            resumeRefresh();
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
    }

    /**
     *launch the activity PlayerListUI: show and manage the playlist 
     */
    private void displayPlaylist() 
    {
        startSubActivity(new Intent(this, PlayListUI.class), 0);
    }

    /**
     * launch the activity ConfigUI.
     */
    private void displayConfig() 
    {
        startSubActivity(new Intent(this, ConfigUI.class), 0);
    }

    /**
     * gets views and initialize them
     */
    private void initializeViews(){        
        //get buttons
        lecture = (Button) findViewById(R.id.lecture);
        next = (Button) findViewById(R.id.next);
        previous = (Button) findViewById(R.id.prec);

        //disable buttons until we are connected to the service
        enableButtons(false);

        //set listeners
        lecture.setOnClickListener(mPlayListener);
        next.setOnClickListener(mNextListener);
        previous.setOnClickListener(mPreviousListener);

        //set focusable
        next.setFocusableInTouchMode(true);
        previous.setFocusableInTouchMode(true);
        lecture.setFocusableInTouchMode(true);

        //get labels
        artiste = (TextView) findViewById(R.id.artiste);
        titre = (TextView) findViewById(R.id.titre);
        //set cover
        cover = (AnimatedCover) findViewById(R.id.cover);
        cover.setImageResource(R.drawable.logo);

        //get progress
        progress = (ProgressView) findViewById(R.id.progress);
        progress.setOnProgressChangeListener(changeListener);
        progress.setTotal(0);
        
        //get image over
        imageOver = (ImageView) findViewById(R.id.imageover);
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
        menu.add(0, PLAYLIST_ID, R.string.menu_playList);
        menu.add(0, OPTION_ID, R.string.menu_option);
        menu.add(0, QUIT_ID, R.string.menu_quit);
        menu.add(0, COVER_ID, R.string.menu_cover_manager);
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
                startSubActivity(new Intent(this, AlbumUI.class), 0);
                break;
        }
        return true;
    }
    /* --------------------- END UI menu -------------------------------------*/

    /* ---------------------  UI actions listener ----------------------------*/

    /*
     * Manage when the progress bar is clicked.
     * If a music was played or paused, the song is played at the new position
     * If the service was stopped, but the playlist filled, we launch the play
     */
    private OnProgressChangeListener changeListener = new OnProgressChangeListener() {
        public void onProgressChanged(View v, int newPos) {
            try{
                if( ! mService.setCurrentPosition(newPos) ) {
                    progress.setProgress(0);
                }
                //relaunch timer if we are playing
                if( mService.getState() == Music.State.PLAYING )
                {
                    //remove timer
                    mTimeHandler.removeCallbacks(timerTask);
                    maxTimer = mService.getDuration();
                    // add timer task to ui thread
                    mTimeHandler.post(timerTask);
                }

            }
            catch(DeadObjectException ex){}
        }
    };

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onTouchEvent(android.view.MotionEvent)
     */
    public boolean onTouchEvent(MotionEvent ev){
        int action = ev.getAction();
        lecture.requestFocus();

        if(action == MotionEvent.ACTION_DOWN){
            if((ev.getEventTime()-firstClickTime) < gapDbClickTime){
                if( Math.sqrt( Math.pow(firstClickPosX-ev.getRawX(),2)+Math.pow(firstClickPosY-ev.getRawY(),2)) < gapDbClick){
                    playPauseAction();
                }
            }
            firstClickPosX = ev.getRawX();
            firstClickPosY = ev.getRawY();
            firstClickTime = ev.getEventTime();
        }
        
        /*touch screen released: play next/previous song or display playlistUI*/
        if(action == MotionEvent.ACTION_UP){
            /*if the Y gap is small and less than on second between the mouse down and up*/
            if( Math.abs(ev.getRawY()-firstClickPosY) < gapSmall && (ev.getEventTime()-firstClickTime) < gapTime){ 
                float gap =  ev.getRawX() - firstClickPosX;
                /*move from the left to the right*/
                if( gap > gapLong && next.isEnabled()){
                    Log.v(TAG,"DROIT -->");
                    try{
                        mService.next();    
                    }
                    catch(DeadObjectException ex){
                        Log.v(TAG, ex.toString());
                    }
                    //showing image to inform the user we have recognized its move
                    imageOver.setImageResource(R.drawable.next_notification);
                    imageOver.startAnimation(imageOverAnim);
                }
                /*move from the right to the left*/
                if( gap < -1*gapLong && previous.isEnabled()){
                    Log.v(TAG, "<--GAUCHE");
                    try{
                        mService.prev();
                    }
                    catch(DeadObjectException ex){
                        Log.v(TAG, ex.toString());
                    }
                    //showing image to inform the user we have recognized its move
                    imageOver.setImageResource(R.drawable.prev_notification2);
                    imageOver.startAnimation(imageOverAnim);
                }
            }
            else if( Math.abs(ev.getRawX()-firstClickPosX) < gapSmall && (ev.getEventTime()-firstClickTime) < gapTime){
                float gap =  ev.getRawY() - firstClickPosY;
                /*move from the up to the down*/
                if( gap > gapLong){
                    Log.v(TAG,"Down -->");
                    displayPlaylist();
                }
                /*we can make on if loop,abs(gap) > gapLong, but if in the futur up->down and down->up are to different movement, each has its own loop*/
                /*move from the down to the up*/
                if( gap < -1*gapLong){
                    Log.v(TAG, "<--UP");
                    displayPlaylist();
                }       
            }
        }
        return true;
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
                if(next.isEnabled()){
                    try{
                        mService.next();
                        next.setBackground(android.R.drawable.btn_default);
                    }catch(DeadObjectException doe){
                        Log.v(TAG, doe.toString());
                    }
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER :
                if(lecture.isEnabled()){
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
     * Listener for the Button Next. Play the next song in the playlist
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
     * Listener for the button previous. Play the previous song in the playlist
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


    /* --------------------- END UI actions listener -------------------------*/

    /**
     * when lecture button is used
     */
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

    /**
     * refresh elapsed time, only once !
     */
    private void timerRefresh(){
        try{
            int time = mService.getCurrentPosition();
            progress.setProgress(time);
        }catch( DeadObjectException doe){
            Log.v(TAG, doe.toString());
        }
    }

    /**
     * refresh lecture button & timer
     */
    private void playRefresh(){
        lecture.setText(R.string.pause);
        //update of the current time displayed
        mTimeHandler.removeCallbacks(timerTask);
        try {
            maxTimer = mService.getDuration();
        } catch (DeadObjectException doe) {
            Log.v(TAG, doe.toString());
        }
        mTimeHandler.post(timerTask);
    }

    /**
     * refresh lecture button & timer
     */
    private void pauseRefresh(){
        lecture.setText(R.string.play);
        // stop timer update
        mTimeHandler.removeCallbacks(timerTask);
    }

    /**
     * refresh song information & buttons next, previous
     */
    private void songRefresh(Move sense){
        try{
            // refresh total time, song info
            int pos = mService.getCurrentSongIndex();
            int plSize = mdb.getPlaylistSize();
            // set total time
            int time = mService.getDuration();
            progress.setTotal(time);
            progress.initializeProgress();

            // ensure that playlist isn't empty
            if (pos >= 1 && pos <= plSize){
                String [] songInfo = mdb.getSongInfoFromCP(pos);
                titre.setText(songInfo[0]);
                artiste.setText(songInfo[1]);
                //if the song has a cover and it's not the cover which is displayed
                if( songInfo[2] != null && !songInfo[2].equals(pathCover)){
                    pathCover=songInfo[2];
                    cover.setImageDrawable(Drawable.createFromPath(pathCover), sense);
                }
                else if(songInfo[2] == null){ //displayed default logo
                    cover.setImageDrawable(getResources().getDrawable(R.drawable.logo), sense);
                    pathCover = null;
                }
            }// buttons next & previous update

            if(mService.getPlayMode() != Music.Mode.RANDOM){
                // when in random mode, next and previous can be used
                if(pos <= 1){
                    // disable previous button
                    previous.setEnabled(false);
                }else{
                    previous.setEnabled(true);
                }

                if(plSize <= 0 || pos == plSize){
                    next.setEnabled(false);
                }else{
                    next.setEnabled(true);
                }
            }
        }catch( DeadObjectException doe){
            Log.v(TAG, doe.toString());
        }
        
        //update play/pause button and timer
        playRefresh();
    }

    /**
     * when there is no song to display
     */
    private void noSongRefresh(){
        // remove timer
        mTimeHandler.removeCallbacks(timerTask);
        // reset all 
        progress.setTotal(0);
        progress.setProgress(0);
        progress.initializeProgress();
        artiste.setText(R.string.artiste);
        titre.setText(R.string.titre);
        lecture.setText(R.string.play);
        cover.setImageDrawable(getResources().getDrawable(R.drawable.logo), Move.NO_ANIM);
        pathCover = null;
        enableButtons(false);
    }

    private void resumeRefresh(){
        // refresh ui when displaying the activity if service connection already made
        // TODO what if service not connected ? faire deux fois ? onconnect & ici ?
        try{
            // refresh considering player state
            switch(mService.getState()){
                case Music.State.PLAYING :
                    enableButtons(true);
                    playRefresh();
                    songRefresh(Move.NO_ANIM);
                    break;
                case Music.State.PAUSED :
                    // we still have to refresh timer once
                    enableButtons(true);
                    pauseRefresh();
                    songRefresh(Move.NO_ANIM);
                    timerRefresh();
                    break;
                case Music.State.END_PLAYLIST_REACHED :
                    // we have nothing to display 
                    noSongRefresh();
                    break;
                case Music.State.STOPPED :
                    enableButtons(true);
                    songRefresh(Move.NO_ANIM);
                    break;
                default :
                    noSongRefresh();
                    break;
            }
        }catch(DeadObjectException doe){
            Log.v(TAG, doe.toString());
        }
    }

}
