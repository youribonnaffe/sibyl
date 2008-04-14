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

package com.sibyl;

import java.io.IOException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDiskIOException;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.util.Log;

import com.sibyl.ui.IPlayerUI;


public class Sibylservice extends Service
{
    
    private MediaPlayer mp;
    private MusicDB mdb;
    private int playerState;
    private boolean repAll;
    private boolean looping;
    private int currentSong;
    private IPlayerUI uiHandler;
    private NotificationManager nm;

    /** creation of the service */
    @Override
    protected void onCreate()
    {        
        /* initialization of the state of the service */
        playerState=Music.State.STOPPED;
        
        // retrieve preferences
        SharedPreferences prefs = getSharedPreferences(Music.PREFS, MODE_PRIVATE);
        currentSong= prefs.getInt("currentSong", 1);
        repAll = prefs.getBoolean("repAll", false);
        looping = prefs.getBoolean("looping", false);

        /* creating MediaPlayer to play music */
        mp = new MediaPlayer();
        mp.setOnCompletionListener(endSongListener);
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        //create or connect to the Database
        try{
            mdb = new MusicDB(this);
            updateNotification(R.drawable.pause,"Sibyl, mobile your music !");
            // load current song
            preparePlaying(mdb.getSong(currentSong));
            
        }catch (SQLiteDiskIOException e){
            Log.v("SibylService", e.getMessage());
            // what sould we do ? updateNotification ?
        }
    }
    
    /**
    * to be corrected by the right usage of idIcon
    * Updates the text of the notification identified by idIcon with the value
    * of the text argument
    * 
    * @param idIcon     identifier of the notification
    * @param text       new text to be displayed by the notification manager
    */
    public void updateNotification(int idIcon, String text)
    {
        Intent appIntent = new Intent(this, com.sibyl.ui.PlayerUI.class);
        
        nm.notify(
                R.layout.notification,                  // we use a string id because it is a unique
                                                   // number.  we use it later to cancel the
                                                   // notification
                new Notification(
                    this,                               // our context
                    idIcon,                             // the icon for the status bar
                    text,                               // the text to display in the ticker
                    System.currentTimeMillis(),         // the timestamp for the notification
                    Music.APPNAME,                            // the title for the notification
                    text,                               // the details to display in the notification
                    appIntent,                               // the contentIntent (see above)
                    R.drawable.icon,                    // the app icon
                    getText(R.string.app_name),         // the name of the app
                    appIntent)); // the appIntent (see above)

    }
    
    @Override
    protected void onDestroy()
    {
        mp.stop();
        mp.release();
        nm.cancel(R.layout.notification);
        // save preferences
        SharedPreferences.Editor prefs = getSharedPreferences(Music.PREFS, MODE_PRIVATE).edit();
        prefs.putInt("currentSong", currentSong);
        prefs.putBoolean("repAll", repAll);
        prefs.putBoolean("looping", looping);
        prefs.commit();
        
    }
    
    /**
    * Resets the media player, sets its source to the file given by the filename 
    * parameter
    * After having called this method, the media player is ready to play.
    *
    * @param filename   name of the file which should be played by the media player
    *   when play is called
    */
    protected void preparePlaying(String filename)
    {
        try 
        {
            mp.reset();
            mp.setDataSource(filename);
            mp.prepare();
        }
        catch ( IOException ioe) 
        {
            Log.v(TAG, ioe.toString());
        }
        catch (IllegalArgumentException iae){
            Log.v(TAG, iae.toString());
        }
    }
    
    /**
    * Starts playing song at the current position in the playlist. If the player 
    * was in pause, the paused song is resumed.
    * If the end of the playlist is reached and playlist repetition is false, 
    * the service will be in the state END_PLAYLIST_REACHED, and the UI will be
    * informed.
    * If an error occured, no song will be played and the service will be in the
    * error state.
    *
    * @return   true in case of success, false otherwise
    */
    protected boolean play() 
    {
        if( playerState != Music.State.PAUSED ) 
        {// if we are not in pause we prepare the mediaplayer to play the next song
            String filename = mdb.getSong(currentSong);
            if( filename == null ) 
            {
                int playListSize = mdb.getPlaylistSize();
                if ( currentSong > playListSize && playListSize > 0 )
                {
                    //end of playlist
                    if( !repAll ) 
                    {
                        playerState = Music.State.END_PLAYLIST_REACHED;
                        currentSong = 1;
                        try 
                        {//the UI is informed that the end of the playlist has been reached
                            uiHandler.handleEndPlaylist();
                        } catch (DeadObjectException e) 
                        {
                            e.printStackTrace();
                        }
                        //don't play anything
                        return false;
                    }
                    else 
                    {// we restart playing from the beginning of the playlist
                        currentSong = 1;
                        return play();
                    }
                }
                else 
                {
                    // error
                    playerState = Music.State.ERROR;
                    return false;
                }
            }
            // Stop mediaplayer so that if it was playing, it doesn't play anymore
            // in the other states, it does nothing
            mp.stop();
            // filename OK so preparing playing of this file
            preparePlaying(filename);
        }
        
        // Start playing if it wasn't paused or continue playing the paused song
        // if it was in pause 
        mp.start();
        playerState=Music.State.PLAYING;
        
        try 
        {// informs UI that we start playing a song
            uiHandler.handleStartPlaying();
        } catch (DeadObjectException e) 
        {
            e.printStackTrace();
        }
        
        // Updating notification
        String [] songInfo = mdb.getSongInfoFromCP(currentSong);
        if( songInfo == null )
        {
            //what to do??? is the error critical or not important and so can be skipped???
        }
        updateNotification(R.drawable.play, songInfo[0]+"-"+songInfo[1]);
        
        return true;
    }
    
    /**
    * Stops playing and puts the service in the stopped state
    *
    */
    protected void stop()
    {
        mp.stop();
        playerState=Music.State.STOPPED;
    }
    
    /**
    * Plays the following song in the playlist
    *
    */
    protected void play_next() 
    {
        int add = 1;
        if( playerState == Music.State.STOPPED) { //we can launch the player with the button next. it must play
            add = 0;                               // the first song of the playlist the first time.
        }
        stop();
        Log.v(TAG,">>> Play_next() called: currentSong="+currentSong);
        currentSong += add;
        if( !play()){ //cancel the changement if nothing is played
            currentSong -=add;
        }
            
    }
    
    /**
    * Plays the previous song in the playlist
    * If we are at the first song of the playlist, the first song will be played
    * from the beginning.
    */
    protected void play_prev() 
    {
        stop();
        Log.v(TAG,">>> Play_prec() called: currentSong="+currentSong);
        if ( currentSong > 1 )
        {
            currentSong--; 
        }
        if( ! play()){  //cancel the changement if nothing is played
            currentSong++;
        }
    }
    
    /**
    * Sets the current position in the playlist to i, and plays the song at the 
    * current position.
    *
    * @param i  new position in the playlist
    */
    protected void playNumberI(int i)
    {
        stop();
        currentSong = i;
        play();
    }

    @Override
    public IBinder onBind(Intent i)
    {
        return mBinder;
    }
    
    //interface accessible par les autres classes (cf aidl)
    private final ISibylservice.Stub mBinder = new ISibylservice.Stub() {
        
        public void connectToReceiver(IPlayerUI receiver) {
            uiHandler=receiver;
        }

        public void start() {
            play();
        }
        
        public void stop() {
            mp.stop();
            playerState=Music.State.STOPPED;
            currentSong = 1; /*initialize currentSong for the next launch of the service*/
        }
        
        public void pause() {
            mp.pause();
            playerState=Music.State.PAUSED;
            updateNotification(R.drawable.pause,"pause");
        }
        
        public int getState() {
            return playerState;
        }
        
        public boolean isPlaying() {
            return playerState==Music.State.PLAYING;
        }
        
        public int getCurrentSongIndex() {
            return currentSong;
        }
        
        public int getCurrentPosition() {
            return mp.getCurrentPosition();
        }
        
        public int getDuration() {
            return mp.getDuration();
        }
        
        public void setCurrentPosition(int msec) {
            mp.seekTo(msec);
        }
        
        public void setLooping(boolean loop) 
        {
            Log.v(TAG,"set looping :"+loop);
            repAll = false;
            looping = loop;
            mp.setLooping(loop ? 1 : 0);//setLooping is waiting for an int
            //and java doesn't do the implicit conversion from bool to int
        }
        
        public int getLooping(){
            if(repAll) return 2;
            return looping ? 1 : 0;
        }
        
        public void next() {
            play_next();
        }
        
        public void prev() {
            play_prev();
        }

        public void playSongPlaylist(int pos) 
        {
            playNumberI(pos);
        }

        public void setRepeatAll()
        {
            Log.v(TAG,"set repeatALL");
            repAll = true;
            looping = false;
        }
         
    };
    
    private OnCompletionListener endSongListener = new OnCompletionListener() 
    {
        public void onCompletion(MediaPlayer mp) 
        {
            mdb.countUp(currentSong);
            if(!looping)
            {
                play_next();
            }
        } 
    };
   

}
