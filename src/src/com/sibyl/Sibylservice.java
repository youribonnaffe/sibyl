package com.sibyl;

import android.app.Service;
import android.content.Intent;

import android.os.Bundle;
import android.media.MediaPlayer;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;

import android.widget.Toast;


public class Sibylservice extends Service
{
    /** Called with the service is first created. */
    @Override
    protected void onCreate()
    {        
        paused=false;
        
        mp = new MediaPlayer();
    }
    
    @Override
    protected void onStart(int startId, Bundle arguments)
    {
       //playSong("test.mp3");
    }
    
    @Override
    protected void onDestroy()
    {
        mp.stop();
        mp.release();
    }
    
    protected void playSong(String filename) 
    {
        if( !paused ) {
        //we're not in pause so we start playing a new song
            try{
                mp.setDataSource("/tmp/"+filename);
                mp.prepare();
            }
            catch ( Exception e) {
                //remplacant du NotificationManager/notifyWithText
                Toast.makeText(Sibylservice.this, "Exception: "+e, 
                    Toast.LENGTH_SHORT).show();

            }
            mp.start();
            
            //remplacant du NotificationManager/notifyWithText
            Toast.makeText(Sibylservice.this, "Playing song: "+filename, 
                    Toast.LENGTH_SHORT).show();
        }
        else {
        //we're in pause so we continue playing the paused song
            mp.start();
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
            playSong("test.mp3");
        }
        
        public void stop() {
            mp.stop();
        }
        
        public void pause() {
            mp.pause();
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
         
    };
    

    private MediaPlayer mp;
    private boolean paused;

}
