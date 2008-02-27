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

import com.sibyl.*;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.view.Menu;
import android.view.View;
import android.view.Menu.Item;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FilenameFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.util.Log;

public class PlayerUI extends Activity {
	
    ISibylservice mService = null;
    
	private static final int QUIT_ID = Menu.FIRST;
	private static final int PLAYLIST_ID = Menu.FIRST +1;
	private static final int OPTION_ID = Menu.FIRST +2;
	private static final int ADD_ID = Menu.FIRST +3;
	
    private static final String TAG = "COLLECTION";
	
	//view of the ui
	private TextView artiste;
	private TextView titre;
	private TextView tempsEcoule;
	private TextView tempsTotal;
	private Button lecture;
	
	private boolean play = false;
	private boolean pause = false;

	private MusicDB mdb;	//the database
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.main);
        //launch the service.
        launchService();
        //get the views
        artiste = (TextView) findViewById(R.id.artiste);
        titre = (TextView) findViewById(R.id.titre);
        tempsEcoule = (TextView) findViewById(R.id.tpsEcoule);
        tempsTotal = (TextView) findViewById(R.id.tpsTotal);
        lecture = (Button) findViewById(R.id.lecture);
        ImageView cover = (ImageView) findViewById(R.id.cover);
        cover.setImageDrawable(Drawable.createFromPath(Music.MUSIC_DIR+"/cover.jpg"));
        
        //create or connect to the Database
    	try{
    	    mdb = new MusicDB(this);
    	    Log.v(TAG,"BD OK");
    	}catch(Exception ex){
    	    Log.v(TAG, ex.toString()+" Create");
    	}

        lecture.setOnClickListener(mPlayListener);
    }
    
    
    public void launchService()	{
        Bundle args = new Bundle();
        args.putString("filename", "test.mp3");
        bindService(new Intent(PlayerUI.this,
                    Sibylservice.class), mConnection, Context.BIND_AUTO_CREATE);
    	
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, QUIT_ID, R.string.menu_quit);
        menu.add(0, PLAYLIST_ID, R.string.menu_playList);
        menu.add(0, OPTION_ID, R.string.menu_option);
        menu.add(0,ADD_ID, "Add "+Music.MUSIC_DIR);
        return true;
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, Item item) {
        super.onMenuItemSelected(featureId, item);
        switch(item.getId()) {
        case QUIT_ID:
            try {
                mService.stop();
            }//lors de l'appel de fonction de interface (fichier aidl)
            //il faut catcher les DeadObjectException
            catch (DeadObjectException ex) {
            }
            play = false;
            pause = false;
            lecture.setText(R.string.stop);
            break;
        case PLAYLIST_ID:
            //launch the playlist's activity
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


    
    private OnClickListener mPlayListener = new OnClickListener()
    {
        public void onClick(View v)
        {
            if( play) //call if a music is played (pause the music)
            {
            	lecture.setText(R.string.stop);
                try {
                    mService.pause();
                }//lors de l'appel de fonction de interface (fichier aidl)
                //il faut catcher les DeadObjectException
                catch (DeadObjectException ex) {
                }
                pause = true;
            }
            else // to start listening a music or resume.
            {
            	lecture.setText(R.string.play);

	            try {
	                mService.start();
	            }//lors de l'appel de fonction de interface (fichier aidl)
	            //il faut catcher les DeadObjectException
	            catch (DeadObjectException ex) {
	            }
	            if(!pause)
	            {
	            	int length = 0;
	            	int min, sec;
	                try {
	                    length = mService.getDuration();
	                }
	                catch (DeadObjectException ex) {
	                }
	                min = Math.round((float) (length / 1000.0 / 60));
	                sec = Math.round((float) ((length / 1000.0 / 60) - min) * 60);
	                tempsTotal.setText(((min < 10) ? "0":"")+String.valueOf(min)+":"+String.valueOf(sec));
	            	
	            }
	            
	            pause = false;
            }
            play = !play;
        }
    };
    
    
    private void fillBD (String path)
    {
    	// get all mp3 files in path
    	try{
    	    File dir = new File(path);
    	    Log.v(TAG, "Insert");
    	    FilenameFilter filter = new FilenameFilter() {
    		public boolean accept(File dir, String name) {
    		    return name.endsWith(".mp3");
    		}
    	    };
    	    
    	    // insert them in the database    
    	    for(String s : dir.list(filter)){
    		try{
    		   long t = System.currentTimeMillis();
    		   mdb.insert(path+s);
    		   Log.v(TAG, "temps "+(System.currentTimeMillis()-t));
    		}catch(SQLiteException sqle){
    		    Log.v(TAG, "sql" + sqle.toString());
    		}
    	    }
    	}catch(Exception ex){
    	    Log.v(TAG, ex.toString());
    	}
    	
    }
    
    private void fillPlayList()
    {
	    try{
	    	Cursor c = mdb.rawQuery("SELECT ID FROM SONG;",null);
	    	int songID [] = new int[c.count()];
	    	int pos = 0;
		    while(c.next()){
		    	songID[pos++] = c.getInt(0); //there is just one column	    	
		    }
		    mdb.insertPlaylist(songID);
	    }catch(Exception ex){
		    Log.v(TAG, ex.toString());
		}
    }

}