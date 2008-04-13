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
import com.sibyl.MusicDB;
import com.sibyl.R;
import com.sibyl.Sibylservice;


//Main activity: the playeur
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

    private boolean play = false; //indicate if Sibyl is playing a song
    private boolean pause = false; //indicate if a stop is paused  /* TODO what ?!? */
    private boolean stopped = false;

    private MusicDB mdb;    //the database

    //handler to call function when datas are received from the service
    private Handler mServHandler = new Handler();

    // time elapsed when playing a song
    private Handler mHandler = new Handler();
    //thread which shows the elapsed time when a song is played.
    private Runnable timerTask = new Runnable() 
    {
    private int timer;
    public void run() 
    {
        // re adjusting time
        try
        {
        // display time
        timer = mService.getCurrentPosition(); // seems to avoid ANR
        }
        catch(DeadObjectException ex){
        // old value will be kept, if the song can't be played anymore
        // an error should be thrown by the service (or not)
        }
        elapsedTime.setText(DateUtils.formatElapsedTime(timer/1000));
        // again in 0.1s
        mHandler.postDelayed(this, 1000);
    }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) 
    {
    super.onCreate(icicle);
    setContentView(R.layout.main);
    initializeViews();
    //launch the service.
    launchService();
    elapsedTime.setText(DateUtils.formatElapsedTime(0));
    //create or connect to the Database
    try
    {
        mdb = new MusicDB(this);
        //display the artist and the song in the playlist
        /* playlist could be empty
            String [] songInfo = mdb.getSongInfoFromCP(1);
            titre.setText(songInfo[0]);
            artiste.setText(songInfo[1]);*/
    }
    catch(SQLiteDiskIOException e)
    {
        Log.v(TAG, e.getMessage());
        // user sould be warned
    }
    }

    /*
     * get views and initialize them
     */
    private void initializeViews(){        
        //get buttons + set listenners + set focus
        lecture = (Button) findViewById(R.id.lecture);
        next = (Button) findViewById(R.id.next);
        previous = (Button) findViewById(R.id.prec);
        avance = (Button) findViewById(R.id.avance);
        /*lecture.setEnabled(false);
        next.setEnabled(false);
        previous.setEnabled(false);*/
        lecture.setOnClickListener(mPlayListener);
        next.setOnClickListener(mNextListener);
        previous.setOnClickListener(mPreviousListener);
        avance.setOnClickListener(mAvanceListener);
        next.setFocusableInTouchMode(true);
        previous.setFocusableInTouchMode(true);
        lecture.setFocusableInTouchMode(true);
        
        //get labels
        artiste = (TextView) findViewById(R.id.artiste);
        titre = (TextView) findViewById(R.id.titre);
        elapsedTime = (TextView) findViewById(R.id.tpsEcoule);
        tempsTotal = (TextView) findViewById(R.id.tpsTotal);
       
        //set cover
        ImageView cover = (ImageView) findViewById(R.id.cover);
        cover.setImageDrawable(Drawable.createFromPath("/data/music/cover.jpg"));  
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
    stopped = true;
    mHandler.removeCallbacks(timerTask); // stop the timer update
    }

    @Override
    protected void onRestart() 
    {
    super.onRestart();
    stopped = false;
    try 
    {
        if(mService.isPlaying())
        {
        mHandler.post(timerTask);
        lecture.setText(R.string.pause);
        play = true;
        pause = false;
        }
        else
        {
        lecture.setText(R.string.play);
        play = true;
        pause = false;
        }
    } catch (DeadObjectException e) 
    {
        Log.v(TAG, e.toString());
        // user should be warned
    }
    }

    //launch the service
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
        }//lors de l'appel de fonction de interface (fichier aidl)
        //il faut catcher les DeadObjectException
        catch (DeadObjectException ex) {
        Log.v(TAG, ex.toString());
        // user should be warned
        }
        play = false;
        pause = false;
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
    }
    };


    //set the total time of the song which is played
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


    //listener for the button Play/Pause
    private OnClickListener mPlayListener = new OnClickListener()
    {
    public void onClick(View v)
    {
        if( play) //call if a music is played (pause the music)
        {
        try 
        {
            mService.pause();
            lecture.setText(R.string.play);
            pause = true;
            // remove timer task from ui thread
            mHandler.removeCallbacks(timerTask);
        }
        catch (DeadObjectException ex) {
            Log.v(TAG, ex.toString());
        }
        }
        else // to start listening a music or resume.
        {
        try 
        {
            mService.start();
            lecture.setText(R.string.pause);
            //updateUI(); //display informations about the song
            mHandler.post(timerTask);
            pause = false;
        }
        catch (DeadObjectException ex) {
            Log.v(TAG, ex.toString());
        }
        }
        play = !play;
    }
    };


    //Listenner for the Button Next. Play the next song in the playlist
    private OnClickListener mNextListener = new OnClickListener()
    {
    public void onClick(View v)
    {
        playSong(PLAY.NEXT);
    }
    };

    //Listenner for the button previous. Play the previous sont in the playlist
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
        mHandler.removeCallbacks(timerTask);
        mHandler.post(timerTask);
        }
        catch (DeadObjectException ex){
        Log.v(TAG, ex.toString());
        // user should be warned
        }
    }
    };

    //launch the activity PlayerListUI: show and manage the playlist
    private void displayPlaylist() 
    {
    Intent i = new Intent(this, PlayListUI.class);
    startSubActivity(i, 0);
    }


    private void displayConfig() 
    {
    Intent i = new Intent(this, ConfigUI.class);
    startSubActivity(i, 0);
    }

    //display the artist, the song, the new total time
    private void updateUI() 
    {
    try 
    {
        elapsedTime.setText(DateUtils.formatElapsedTime(0));//reset the time
        setTotalTime();
        int pos=0;
        //display the song and artist name
        pos=mService.getCurrentSongIndex();
        Log.v(TAG, "updateUI: pos="+pos);
        String [] songInfo = mdb.getSongInfoFromCP(pos);
        titre.setText(songInfo[0]);
        artiste.setText(songInfo[1]);
        //remove timer
        mHandler.removeCallbacks(timerTask);
        // add timer task to ui thread
        if (!stopped)
        {
        mHandler.post(timerTask);
        }
    }
    catch (DeadObjectException ex){
        Log.v(TAG, ex.toString());
        // ui changes ?
    }
    }

    /* TODO Supprimer la communication avec le service quand la fenetre n'est plus affichée ? */
    //communication from the service
    private final IPlayerUI.Stub mServiceListener = new IPlayerUI.Stub() 
    {
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
            noSongToPlay();
        }
        });
    }
    };

    private void noSongToPlay()
    {
    mHandler.removeCallbacks(timerTask);
    elapsedTime.setText(DateUtils.formatElapsedTime(0));
    artiste.setText(R.string.artiste);
    tempsTotal.setText(R.string.time_zero);
    titre.setText(R.string.titre);
    lecture.setText(R.string.play);
    pause = true;
    play = false;
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
        previous.performClick();
        previous.setBackground(android.R.drawable.btn_default);
        //lecture.requestFocus();
        return true;
    case KeyEvent.KEYCODE_DPAD_RIGHT :
        next.performClick();
        next.setBackground(android.R.drawable.btn_default);
        //lecture.requestFocus();
        return true;
    case KeyEvent.KEYCODE_DPAD_CENTER :
        //lecture.requestFocus();
        lecture.performClick();
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
        previous.setBackground(android.R.drawable.btn_default_selected);
        return true;
    case KeyEvent.KEYCODE_DPAD_RIGHT :
        next.setBackground(android.R.drawable.btn_default_selected);
        return true;
    case KeyEvent.KEYCODE_DPAD_CENTER :
        return true;
    }
    return super.onKeyDown(keycode, event);
    }

    //call the service methods prev() or next() in function of type
    //and refresh the UI
    private void playSong( int type){
    try
    {
        if( type == PLAY.NEXT) {
        mService.next();
        }
        else{
        mService.prev();
        }
        if( mService.isPlaying())
        {
        play = true;
        pause = false;
        lecture.setText(R.string.pause);
        //updateUI();
        }
    }
        catch (DeadObjectException ex) {
            Log.v(TAG, ex.toString());
            // user warned ?
    }
    }

}
