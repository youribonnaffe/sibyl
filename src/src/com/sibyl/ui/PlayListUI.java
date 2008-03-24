package com.sibyl.ui;

import com.sibyl.ISibylservice;
import com.sibyl.MusicDB;
import com.sibyl.R;
import com.sibyl.Sibylservice;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Menu.Item;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
//import android.widget.TextView;

public class PlayListUI extends ListActivity
{
    private static final int ADD_ID = Menu.FIRST;
    private static final int NEW_ID = Menu.FIRST +1;
    private static final int SMARTPL_ID = Menu.FIRST +2;
    private static final int BACK_ID = Menu.FIRST +3;
    
    private static final String TAG = "PLAYLIST";
    
    ISibylservice mService = null;
    
    //private TextView playList;
    
    private MusicDB mdb;    //the database
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) 
    {
        super.onCreate(icicle);
        launchService();
        setContentView(R.layout.playlist);
        
        try
        {
            mdb = new MusicDB(this);
            Log.v(TAG,"BD OK");
        }
        catch(Exception ex)
        {
            Log.v(TAG, ex.toString()+" Create");
        }   
        fillData();
    }
    
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        super.onCreateOptionsMenu(menu);
        menu.add(0, ADD_ID, R.string.menu_add);
        menu.add(0, NEW_ID, R.string.menu_new);
        menu.add(0, SMARTPL_ID, R.string.menu_smartpl);
        menu.add(0, BACK_ID, R.string.menu_back);
        return true;
    }
    
    @Override
    protected void onDestroy() 
    {
        super.onDestroy();
        unbindService(mConnection);        
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, Item item) 
    {
        super.onMenuItemSelected(featureId, item);
        switch(item.getId()) 
        {
        case ADD_ID:
            break;
        case NEW_ID:
            try 
            {
                mService.stop();
                mdb.clearPlaylist();
                fillData();
            } catch (DeadObjectException e) {
                e.printStackTrace();
            }
            break;
        case SMARTPL_ID:
            break;
        case BACK_ID:
            finish();
            break;
        }
        
        return true;
    }
    
    
    
    private void fillData()
    {
        try
        {
            Log.v(TAG,"Curseur creation");
            Cursor c = mdb.getPlayListInfo();
            startManagingCursor(c);

            try{
                /* TODO l'ui doit pas connaitre les champs de la base, donc a changer*/
            ListAdapter adapter = new SimpleCursorAdapter(
                    this, R.layout.playlist_row, c, new String[] {"_id","artist_name"},  
                    new int[] {R.id.text1, R.id.text2});  
            setListAdapter(adapter);
            }
            catch(Exception ex)
            {
                Log.v(TAG, ex.toString());
            }
        }
        catch(Exception ex)
        {
            Log.v(TAG, ex.toString());
        }
        
    }
    
    /* TODO verifier que l'on se connecte bien au meme service que PlayerUI 
     * et qu'il n'y a pas 2 servi lancés, normalement c'est bon */
    
    public void launchService() 
    {
        bindService(new Intent(PlayListUI.this,
            Sibylservice.class), mConnection, Context.BIND_AUTO_CREATE);
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
            try 
            {
                /* Positionner le selecteur de la liste sur la chanson en cours
                 * une fois que le service est connecté
                 * */
                setSelection(mService.getCurrentSongIndex()-1);
            } 
            catch (DeadObjectException e) 
            {
                e.printStackTrace();
            }
        }

        public void onServiceDisconnected(ComponentName className)
        {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;      
        }
    };
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) 
    {
        Log.v(TAG,"test <<<<<<<<<<<< >>>>>>>>>>>>>"+position);
        position ++;
        try 
        {
            mService.playSongPlaylist(position);
        } catch (DeadObjectException e) 
        {
            e.printStackTrace();
        }
    }
    

    
}
