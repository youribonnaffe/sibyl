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
import java.io.FilenameFilter;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.util.DateUtils;
import android.util.Log;
import android.view.Menu;
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

public class PlayerUI extends Activity
{

    ISibylservice mService = null;

    private static final int QUIT_ID = Menu.FIRST;
    private static final int PLAYLIST_ID = Menu.FIRST +1;
    private static final int OPTION_ID = Menu.FIRST +2;
    private static final int ADD_ID = Menu.FIRST +3;

    private static final String TAG = "COLLECTION";

    //view of the ui
    private TextView artiste;
    private TextView titre;
    private TextView elapsedTime;
    private TextView tempsTotal;
    private Button lecture;

    private boolean play = false; //indicate if Sibyl is playing a song
    private boolean pause = false; //indicate if a stop is paused
    private Button next;
    private Button previous;
    private Button avance;

    private MusicDB mdb;	//the database
    
    //handler to call function when datas are received from the service
    private Handler mServHandler = new Handler();
    
    // time elapsed when playing a song
    private int time;
    private Handler mHandler = new Handler();
    private Runnable timerTask = new Runnable() 
    {
        public void run() 
        {
            // re adjusting time
            if( time%10 == 0)
            {
                try
                {
                    time = mService.getCurrentPosition()/1000;
                }
                catch(DeadObjectException ex){}
            }
            // display time
            elapsedTime.setText(DateUtils.formatElapsedTime(time));
            time++;
            // re update timer in 1 sec
            mHandler.postDelayed(this, 1000);
        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) 
    {
        super.onCreate(icicle);
        setContentView(R.layout.main);
        //launch the service.
        launchService();
        //get the views
        artiste = (TextView) findViewById(R.id.artiste);
        titre = (TextView) findViewById(R.id.titre);
        elapsedTime = (TextView) findViewById(R.id.tpsEcoule);
        tempsTotal = (TextView) findViewById(R.id.tpsTotal);
        lecture = (Button) findViewById(R.id.lecture);
        lecture.setOnClickListener(mPlayListener);
        next = (Button) findViewById(R.id.next);
        previous = (Button) findViewById(R.id.prec);
        avance = (Button) findViewById(R.id.avance);
        //set cover
        ImageView cover = (ImageView) findViewById(R.id.cover);
        cover.setImageDrawable(Drawable.createFromPath("/data/music/cover.jpg"));  
        //set listenner
        lecture.setOnClickListener(mPlayListener);
        next.setOnClickListener(mNextListener);
        previous.setOnClickListener(mPreviousListener);
        avance.setOnClickListener(mAvanceListener);
        
        //create or connect to the Database
        try
        {
            mdb = new MusicDB(this);
            Log.v(TAG,"BD OK");
            titre.setText(mdb.getSongNameFromCP(1));
            artiste.setText(mdb.getArtistNameFromCP(1));
        }
        catch(Exception ex)
        {
            Log.v(TAG, ex.toString()+" Create");
        }
        
    }

    @Override
    protected void onDestroy() 
    {
        super.onDestroy();
        mHandler.removeCallbacks(timerTask);
    }

    public void launchService()	
    {
        Bundle args = new Bundle();
        args.putString("filename", "test.mp3");
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
        menu.add(0,ADD_ID, "Add "+Music.MUSIC_DIR);
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
            catch (DeadObjectException ex) {}
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
            break;
        case ADD_ID:
            //add song
            fillBD(Music.MUSIC_DIR+"/");
            fillPlayList();
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

            //remplacant du NotificationManager/notifyWithText
            Toast.makeText(PlayerUI.this, "Connexion au service reussie", 
                    Toast.LENGTH_SHORT).show();
                    
            //connection of the service to the activity
            try {
                mService.connectToReceiver(mServiceListener);
            }
            catch(DeadObjectException ex){}

        }

        public void onServiceDisconnected(ComponentName className)
        {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;

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
        catch (DeadObjectException ex){}
    }

    private OnClickListener mPlayListener = new OnClickListener()
    {
        public void onClick(View v)
        {
            if( play) //call if a music is played (pause the music)
            {
                lecture.setText(R.string.play);
                try 
                {
                    mService.pause();
                }//lors de l'appel de fonction de interface (fichier aidl)
                //il faut catcher les DeadObjectException
                catch (DeadObjectException ex) {}
                pause = true;
                // remove timer task from ui thread
                mHandler.removeCallbacks(timerTask);
            }
            else // to start listening a music or resume.
            {
                lecture.setText(R.string.pause);
                try 
                {
                    mService.start();
                }//lors de l'appel de fonction de interface (fichier aidl)
                //il faut catcher les DeadObjectException
                catch (DeadObjectException ex) {}
                if(!pause) //a song is start.
                {
                    setTotalTime();
                }
                // reset timer so it will be recalculated if resuming
                time = 0;
                mHandler.removeCallbacks(timerTask);
                // add timer task to ui thread
                mHandler.post(timerTask);
                pause = false;
            }
            play = !play;
        }
    };
    
    
    //Listenner for the Button Next. Play the next song in the playlist
    private OnClickListener mNextListener = new OnClickListener()
    {
        public void onClick(View v)
        {
            try
            {
                mService.next();
                if( mService.isPlaying())
                {
                    mHandler.removeCallbacks(timerTask);
                    time = 0;
                    // add timer task to ui thread
                    mHandler.post(timerTask);
                    play = true;
                    pause = false;
                    lecture.setText(R.string.pause);
                    updateUI();
                }
            }
            catch (DeadObjectException ex){}
        }
    };
    
	//Listenner for the button previous. Play the previous sont in the playlist
    private OnClickListener mPreviousListener = new OnClickListener()
    {
        public void onClick(View v)
        {
            try
            {
                mService.prev();
                if( mService.isPlaying()) //if a song is really player, update time, artist,name.
                {
                    mHandler.removeCallbacks(timerTask);
                    time = 0;
                    // add timer task to ui thread
                    mHandler.post(timerTask);
                    play = true;
                    pause = false;
                    lecture.setText(R.string.pause);
                    updateUI();
                }
            }
            catch (DeadObjectException ex){}
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
                time=0;
                mHandler.post(timerTask);
            }
            catch (DeadObjectException ex){}
        }
    };
    
    
    private void fillBD (String path)
    {
        // get all mp3 files in path
        try
        {
            File dir = new File(path);
            Log.v(TAG, "Insert");
            FilenameFilter filter = new FilenameFilter() 
            {
                public boolean accept(File dir, String name) 
                {
                    return name.endsWith(".mp3");
                }
            };

            // insert them in the database    
            for(String s : dir.list(filter))
            {
                try
                {
                    long t = System.currentTimeMillis();
                    mdb.insert(path+s);
                    Log.v(TAG, "temps "+(System.currentTimeMillis()-t));
                }
                catch(SQLiteException sqle)
                {
                    Log.v(TAG, "sql" + sqle.toString());
                }
            }
        }
        catch(Exception ex)
        {
            Log.v(TAG, ex.toString());
        }

    }

    private void fillPlayList()
    {
        try
        {
            Cursor c = mdb.rawQuery("SELECT _ID FROM SONG",null);
            int songID [] = new int[c.count()];
            int pos = 0;
            while(c.next())
            {
                songID[pos++] = c.getInt(0); //there is just one column	    	
            }
            mdb.insertPlaylist(songID);
        }
        catch(Exception ex)
        {
            Log.v(TAG, ex.toString());
        }
    }
    
    private void displayPlaylist() 
    {
        Intent i = new Intent(this, PlayListUI.class);
        startSubActivity(i, 0);
    }

    //display the artist, the song, the new total time
    public void updateUI() 
    {
        setTotalTime();
        time = 0;
        int pos=0;
        try {
            pos=mService.getCurrentSongIndex();
            Log.v(TAG, "updateUI: pos="+pos);
            /*Cursor c = mdb.rawQuery("SELECT title, artist_name FROM song, current_playlist, artist "
                            +"WHERE pos="+pos+" AND song._id=current_playlist.id and song.artist=artist.id", null);*/
            /*if(c.first()) {*/
                titre.setText(mdb.getSongNameFromCP(pos));
                artiste.setText(mdb.getArtistNameFromCP(pos));
            /*}*/
        }
        catch (DeadObjectException ex){}
    }
    
    //communication from the service
    private final IPlayerUI.Stub mServiceListener = new IPlayerUI.Stub() {
        public void handleEndSong() {
            Log.v(TAG, "End song handled in PlayerUI");
            mServHandler.post(new Runnable()
            {
                public void run()
                {
                    updateUI();
                }
            });
            
        }
    };

}
