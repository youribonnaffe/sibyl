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
import java.util.EmptyStackException;
import java.util.Random;
import java.util.Stack;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDiskIOException;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.IBinder;
import android.os.RemoteException;

public class Sibylservice extends Service
{

    private MediaPlayer mp;
    private MusicDB mdb;
    private int playerState;
    private int playMode;
    private int loopMode;
    private Stack<Integer> songHistory;
    private Random randomVal;
    private int currentSong;
    private NotificationManager nm;

    private BroadcastReceiver callFilter = new BroadcastReceiver() {
        private boolean wasPlaying = false;
        private static final String CALL_IN_1 = "RINGING";
        private static final String CALL_IN_2 = "OFFHOOK";
        private static final String CALL_OUT = "IDLE";
        private static final String PHONE_STATE = "state";
        public void onReceive(Context arg0, Intent arg1) {
            //Log.v("CALL", arg1.toString());
            try{
                String state = arg1.getExtras().getString(PHONE_STATE);
                if(state==null) state = "";
                if( (state.equals(CALL_IN_1) || state.equals(CALL_IN_2) ) && playerState == Music.State.PLAYING){
                    // we are receiving a call when playing music
                    mBinder.pause();
                    wasPlaying = true;
                }else if (state.equals(CALL_OUT)){
                    if(wasPlaying && playerState == Music.State.PAUSED){
                        // we were playing music when call occurred
                        mBinder.start();
                    }
                    wasPlaying = false;
                }
            }catch(RemoteException re){
                //Log.v("callFilter", doe.toString());
            }
        }
    };
    
    /** creation of the service */
    @Override
    public void onCreate()
    {        
        /* initialization of the state and mode of the service */
        playerState=Music.State.STOPPED;

        //stack to store the list of played songs
        songHistory=new Stack<Integer>();

        // initialization of the random generator with the current time of day in
        // milliseconds as the initial state (by default in the constructor)
        randomVal=new Random();

        // retrieve preferences
        SharedPreferences prefs = getSharedPreferences(Music.PREFS, MODE_PRIVATE);
        //Log.v("SibylService", prefs.getAll().toString());
        currentSong= prefs.getInt("currentSong", 1);
        loopMode = prefs.getInt("loopMode", Music.LoopMode.NO_REPEAT);
        playMode = prefs.getInt("playMode", Music.Mode.NORMAL);


        //adds the song restored from preferences to the list of played song
        if (playMode == Music.Mode.RANDOM)
        {
            songHistory.push(currentSong);
        }

        /* creating MediaPlayer to play music */
        mp = new MediaPlayer();
        mp.setOnCompletionListener(endSongListener);
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        //create or connect to the Database
        try{
            mdb = new MusicDB(this);
            updateNotification(R.drawable.pause,getResources().getText(R.string.app_name));

            // set player state
            if(mdb.getPlaylistSize() == 0){
                currentSong = 0;
                playerState = Music.State.END_PLAYLIST_REACHED;
            }else{
                // load current song
                preparePlaying();
                playerState = Music.State.PAUSED;
            }

            registerReceiver(callFilter, new IntentFilter(Intent.ACTION_ANSWER));
        }catch (SQLiteDiskIOException e){
            //Log.v("SibylService", e.getMessage());
            // what should we do ? updateNotification ?
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
    public void updateNotification(int idIcon, CharSequence text)
    {
        //Intent appIntent = new Intent(this, com.sibyl.ui.PlayerUI.class);
       /* int timeMilli = 3000;
        nm.notify(R.layout.notification,                  // we use a string id because it is a unique
                // number.  we use it later to cancel the
                // notification
                new Notification(idIcon, text, timeMilli));*/
//                new Notification(
//                        this,                               // our context
//                        idIcon,                             // the icon for the status bar
//                        text,                               // the text to display in the ticker
//                        System.currentTimeMillis(),         // the timestamp for the notification
//                        Music.APPNAME,                      // the title for the notification
//                        text,                               // the details to display in the notification
//                        appIntent,                          // the contentIntent (see above)
//                        R.drawable.logo,                    // the app icon
//                        getText(R.string.titre),            // the name of the app
//                        appIntent));                        // the appIntent (see above)

    }

    @Override
    public void onDestroy()
    {
        mp.stop();
        mp.release();
        nm.cancel(R.layout.notification);
        // save preferences
        SharedPreferences.Editor prefs = getSharedPreferences(Music.PREFS, MODE_PRIVATE).edit();
        prefs.putInt("currentSong", currentSong);
        prefs.putInt("loopMode", loopMode);
        prefs.putInt("playMode", playMode);
        prefs.commit();
        unregisterReceiver(callFilter);
    }

    /**
     * Resets the media player, sets its source to the file given by currentSong
     * position
     * After having called this method, the media player is ready to play.
     */
    protected void preparePlaying()
    {
        try 
        {
            mp.reset();
            // since it is always called with currentSong parameter has been removed
            mp.setDataSource(mdb.getSong(currentSong));
            mp.prepare();
        }
        catch ( IOException ioe) 
        {
            //Log.v(TAG, ioe.toString());
        }
        catch (IllegalArgumentException iae){
            //Log.v(TAG, iae.toString());
        }
    }

    /**
     * Starts playing song at the current position in the playlist. If the player 
     * was in pause, the paused song is resumed.
     * If the end of the playlist is reached and playlist repetition is false, 
     * the service will be in the state END_PLAYLIST_REACHED, and the UI will be
     * informed.
     * If an error occurred, no song will be played and the service will be in the
     * error state.
     * 
     * @param intentType    string defining the intent to send, when the lecture starts, if
     * it is null, Music.Action.PLAY is sent
     *
     * @return   true in case of success, false otherwise
     */
    protected boolean play(String intentType) 
    {
        if( playerState != Music.State.PAUSED ) 
        {// if we are not in pause we prepare the media player to play the next song

            int plSize = mdb.getPlaylistSize();
            if(currentSong <= plSize){
                // changing song
                mp.stop();
                // filename OK so preparing playing of this file
                preparePlaying();
                // next will be to start playing the song
            } else {
                // end of playlist
                if( loopMode == Music.LoopMode.REPEAT_PLAYLIST){
                    currentSong = 1;
                    return play(intentType);
                } else {
                    // end of playlist, stop playing
                    playerState = Music.State.END_PLAYLIST_REACHED;
                    //currentSong = 0;
                    sendBroadcast(new Intent(Music.Action.NO_SONG));
                    return false;
                }
            }
        }
        // Start playing if it wasn't paused or continue playing the paused song
        // if it was in pause 
        mp.start();
        playerState=Music.State.PLAYING;

        if( intentType == null )
        {
            intentType = Music.Action.PLAY;
        }
        sendBroadcast(new Intent(intentType));

        // Updating notification
        String [] songInfo = mdb.getSongInfoFromCP(currentSong);
        if( songInfo == null )
        {
            songInfo = new String[2];
            songInfo[0] = songInfo[1] = getString(R.string.tags_unknown);
        }
        updateNotification(R.drawable.play, songInfo[0]+"-"+songInfo[1]);

        // true because we are actually playing a song
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
     * Plays a song randomly and save its id in the playlist in the list orderList
     *
     * @param intentType    string defining the intent to send, when the lecture starts
     */
    private boolean play_random(String intentType)
    {
        stop();
        currentSong = randomVal.nextInt(mdb.getPlaylistSize())+1;
        // nextInt(int n) returns a random value between 0 (inclusively) and n (exclusively)
        songHistory.push(currentSong);
        return play(intentType);
    }

    /**
     * Plays the next song in the playlist
     *
     */
    protected void play_next() 
    {
        if( playMode == Music.Mode.RANDOM )
        {
            play_random(Music.Action.NEXT);
        }
        else 
        {
            stop();
            currentSong++;
            if( !play(Music.Action.NEXT) ) { //cancel the changement if nothing is played
                // already set if end of playlist
                currentSong--;
            } 
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
        if( playMode == Music.Mode.RANDOM )
        {
            try
            {
                songHistory.pop();//remove current song, we want the previous song.
                currentSong = songHistory.peek();
            }
            catch(EmptyStackException e)
            {
                currentSong = 1;
            }

            play(Music.Action.PREVIOUS);
        }
        else 
        {
            currentSong--; 
            if( ! play(Music.Action.PREVIOUS) ){  //cancel the changement if nothing is played
                currentSong++;
            }
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
        if(playMode == Music.Mode.RANDOM){
            // if we are in random mode, we have to remember the last track played
            songHistory.push(currentSong);
        }
        currentSong = i;
        play(Music.Action.PLAY);
    }

    @Override
    public IBinder onBind(Intent i)
    {
        return mBinder;
    }

    //interface accessible par les autres classes (cf aidl)
    private final ISibylservice.Stub mBinder = new ISibylservice.Stub() {

        public void playlistChange(){
            // if playlist was empty
            if(mdb.getPlaylistSize() > 0){
                currentSong = 1;
                playerState = Music.State.PAUSED;
                preparePlaying();
            }
        }

        public void start() {
            play(Music.Action.PLAY);
        }

        public void clear() {
            mp.stop();
            mdb.clearPlaylist();
            playerState=Music.State.END_PLAYLIST_REACHED;
            currentSong = 0; /*initialize currentSong for the next launch of the service*/
        }

        public void stop() {
            mp.stop();
            playerState=Music.State.STOPPED;
        }

        public void pause() {
            mp.pause();
            playerState=Music.State.PAUSED;
            updateNotification(R.drawable.pause,getResources().getText(R.string.pause));
            // warn ui that we paused music playing
            sendBroadcast(new Intent(Music.Action.PAUSE));
        }

        public int getState() {
            return playerState;
        }

        public int getCurrentSongIndex() {
            return currentSong;
        }

        public int getCurrentPosition() {
            if( playerState == Music.State.END_PLAYLIST_REACHED
                    || playerState == Music.State.STOPPED
                    || playerState == Music.State.ERROR ) {
                return 0;
            }else{
                return mp.getCurrentPosition();
            }
        }

        public int getDuration() {
            if( playerState == Music.State.END_PLAYLIST_REACHED
                    || playerState == Music.State.STOPPED
                    || playerState == Music.State.ERROR ) {
                return 0;
            }else{
                return mp.getDuration();
            }
        }

        public boolean setCurrentPosition(int msec) {
            if( playerState != Music.State.PLAYING
                    && playerState != Music.State.PAUSED ) {
                return false;
            }

            mp.seekTo(msec);
            //because when we move to an other pos the music starts
            if( playerState == Music.State.PAUSED ) {
                mp.pause();
            }
            return true;
        }

        public int getLooping(){
            return loopMode;
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

        public void setLoopMode(int mode)
        {
            //we loop on the song only if mode == REPEAT_SONG
            //mp.setLooping(mode == Music.LoopMode.REPEAT_SONG ? 1 : 0);

            loopMode = mode;
        }

        public void setPlayMode(int mode)
        {
            if(playMode == Music.Mode.RANDOM && mode != Music.Mode.RANDOM){
                // if we are changing from random to another mode, empty history
                songHistory.clear();
            }
            playMode=mode;
        }

        public int getPlayMode(){
            return playMode;
        }

    };

    private OnCompletionListener endSongListener = new OnCompletionListener() 
    {
        public void onCompletion(MediaPlayer mp) 
        {
            mdb.countUp(currentSong);
            if(loopMode == Music.LoopMode.REPEAT_SONG)
            {
                play(Music.Action.PLAY);
            }
            else {
                play_next();
            }
        } 
    };


}
