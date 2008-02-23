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
       // Intent intent = new Intent();
       // intent.setClass(this, SibylUI.class);
    
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
            
        }
        
        public void pause() {
        
        }
    };
    

    private MediaPlayer mp;

}
