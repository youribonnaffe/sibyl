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

import java.util.Observable;
import java.util.Observer;

import android.app.Service;
import android.content.Intent;

import android.os.Bundle;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;

//import android.os.Binder;
import android.os.IBinder;
//import android.os.Parcel;

//import android.widget.Toast;

import android.util.Log;


public class Sibylservice extends Service
{
    /** Called with the service is first created. */
    @Override
    protected void onCreate()
    {        
        paused=false;
        currentSong=0;
        mp = new MediaPlayer();
        mp.setOnCompletionListener(endSongListener);
        obser = new Observable(); 
        
        //create or connect to the Database
    	try{
    	    mdb = new MusicDB(this);
    	    Log.v(TAG,"BD OK");
    	}
        catch(Exception ex){
    	    Log.v(TAG, ex.toString()+" Create");
    	}

    }
    
    @Override
    protected void onStart(int startId, Bundle arguments)
    {

    }
    
    @Override
    protected void onDestroy()
    {
        mp.stop();
        mp.release();
    }
    
    protected void play() {
        if( !paused ) {
            play_next();
        }
        else {
            mp.start();
            paused=false;
        }

    }
    
    protected void play_next() {
        Log.v(TAG,">>> Play_next() called: currentSong="+currentSong);
        String filename=mdb.nextSong(currentSong);
        currentSong++;
        paused=false;
        obser.notifyObservers();
        if(filename != null) playSong(filename);
    }
    
    protected void playSong(String filename) 
    {
        Log.v(TAG,">>> PlaySong("+filename+") called: paused="+ paused);
        if( !paused ) {
        //we're not in pause so we start playing a new song
            try{
                mp.reset();
                mp.setDataSource(/*Music.MUSIC_DIR+"/"+*/filename);
                mp.prepare();
            }
            catch ( Exception e) {
                //remplacant du NotificationManager/notifyWithText
                Log.v(TAG, "playSong: exception: "+e.toString());
                //Notifications removed else they throw an exception
                //surely a problem of multithreading
               /* Toast.makeText(Sibylservice.this, "Exception: "+e.toString(), 
                    Toast.LENGTH_SHORT).show();*/
            }
            mp.start();
            
            //remplacant du NotificationManager/notifyWithText
            /*Toast.makeText(Sibylservice.this, "Playing song: "+filename, 
                    Toast.LENGTH_LONG).show();*/
        }
        else {
        //we're in pause so we continue playing the paused song
            mp.start();
            /*Toast.makeText(Sibylservice.this, "reprise", 
                    Toast.LENGTH_LONG).show();*/
            paused=false;
        }

    }
    

    @Override
    public IBinder onBind(Intent i)
    {
        return mBinder;
    }
    
    //interface accessible par les autres classes (cf aidl)
    private final ISibylservice.Stub mBinder = new ISibylservice.Stub() {
        public void start() {
            play();
        }
        
        public void stop() {
            mp.stop();
        }
        
        public void pause() {
            mp.pause();
            paused=true;
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
        
        public void setLooping(int looping) {
            mp.setLooping(looping);
        }
        
        public void next() {
            play_next();
        }
        
        public void prev() {
        }
        
        public void addObserver(Observer obs)
        {
            obser.addObserver(obs);
        }
        
        void delObserver(Observer obs)
        {
            obser.deleteObserver(obs);
        }
         
    };
    
    private OnCompletionListener endSongListener = new OnCompletionListener() 
    {
        public void onCompletion(MediaPlayer mp) 
        {
            play_next();
        } 
    };
    

    private MediaPlayer mp;
    private MusicDB mdb;
    private boolean paused;
    private int currentSong;
    private Observable obser;

}
