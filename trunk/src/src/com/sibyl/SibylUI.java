package com.sibyl;


import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.content.ComponentName;
import android.os.DeadObjectException;

import android.app.NotificationManager;


import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;


public class SibylUI extends Activity
{
    ISibylservice mService = null;
    
    /** Called with the activity is first created. */
    @Override
    public void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);
        setContentView(R.layout.main);

        Button button = (Button)findViewById(R.id.start);
        button.setOnClickListener(mStartListener);
        
        Button button2 = (Button)findViewById(R.id.play);
        button2.setOnClickListener(mPlayListener);


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
            
            NotificationManager nm = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
            nm.notifyWithText(100,
                      ("Connexion au service reussie"),
                      NotificationManager.LENGTH_SHORT,
                      null);

        }

        public void onServiceDisconnected(ComponentName className)
        {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            
            NotificationManager nm = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
            nm.notifyWithText(100,
                      ("Deconnexion du service"),
                      NotificationManager.LENGTH_SHORT,
                      null);
        }
    };

     
    private OnClickListener mStartListener = new OnClickListener()
    {
        public void onClick(View v)
        {

            Bundle args = new Bundle();
            args.putString("filename", "test.mp3");
            bindService(new Intent(SibylUI.this,
                    Sibylservice.class), 
                    null, mConnection, Context.BIND_AUTO_CREATE);

        }
    };
    
    private OnClickListener mPlayListener = new OnClickListener()
    {
        public void onClick(View v)
        {
            try {
                    mService.start();
            }//lors de l'appel de fonction de interface (fichier aidl)
            //il faut catcher les DeadObjectException
            catch (DeadObjectException ex) {
            
            }
        }
    };
}
