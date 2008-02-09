package com.sibyl;

import android.app.Service;
import android.content.Intent;

import android.os.Bundle;
import android.media.MediaPlayer;


import android.os.BinderNative;
import android.os.IBinder;
import android.os.Parcel;

import android.app.NotificationManager;


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
        NotificationManager nm = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);

        
        try{
            mp.setDataSource("/tmp/"+filename);
        }
        catch ( Exception e) {
            nm.notifyWithText(1235, "Exception: "+e,
                NotificationManager.LENGTH_SHORT, null);

        }
        mp.prepare();
        mp.start();

        nm.notifyWithText(1234, "Playing song: "+filename,
            NotificationManager.LENGTH_SHORT, null);


    }
    
    @Override
    public IBinder getBinder()
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
