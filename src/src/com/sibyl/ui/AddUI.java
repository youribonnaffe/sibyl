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

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Menu.Item;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.sibyl.Music;
import com.sibyl.MusicDB;
import com.sibyl.R;

public class AddUI extends ListActivity 
{
    private static final int BACK_ID = Menu.FIRST;
    
    private static final String TAG = "AddUI";
    private MusicDB mdb;    //the database
    
    private enum STATE { MAIN, ARTIST, ALBUM, STYLE,SONG};

    
    private STATE positionMenu = STATE.MAIN; //position in the menu

    @Override
    protected void onCreate(Bundle icicle) 
    {
        super.onCreate(icicle);
        
        try
        {
            mdb = new MusicDB(this);
            Log.v(TAG,"BD OK");
        }
        catch(Exception ex)
        {
            Log.v(TAG, ex.toString()+" Create");
        }   
       displayMainMenu(); 
    }
    
    @Override
    protected void onDestroy() 
    {
        super.onDestroy();     
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        super.onCreateOptionsMenu(menu);
        menu.add(0, BACK_ID, R.string.menu_back);
        return true;
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, Item item) 
    {
        super.onMenuItemSelected(featureId, item);
        switch(item.getId()) 
        {
        case BACK_ID:
            finish();
            break;
        }
        
        return true;
    }
    
    private void displayMainMenu()
    {
        String[] field = {getString(R.string.add_artist),
                getString(R.string.add_album),
                getString(R.string.add_song),
                getString(R.string.add_style)};
        
        try
        {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.add_row, R.id.text1, field);
            setListAdapter(adapter);
        }
        catch(Exception ex){}      
    }
    
    
    protected void onListItemClick(ListView l, View vu, int position, long id) 
    {
        LinearLayout row = (LinearLayout) vu;
        TextView text = (TextView) row.findViewById(R.id.text1);
        
        if( positionMenu == STATE.MAIN){
                mainMenu(text.getText());
        }
        else
        {   if(positionMenu == STATE.ARTIST){
                mdb.insertPlaylist(Music.SONG.ARTIST, text.getText().toString());
            }
            else if(positionMenu == STATE.ALBUM){
                mdb.insertPlaylist(Music.SONG.ALBUM, text.getText().toString());
            }
            else if(positionMenu == STATE.SONG){
                mdb.insertPlaylist(Music.SONG.TITLE, text.getText().toString());
            }
            else if(positionMenu == STATE.STYLE){
                mdb.insertPlaylist(Music.SONG.GENRE, text.getText().toString());
            }
            positionMenu = STATE.MAIN;
            displayMainMenu();
            Log.v(TAG,text.toString());
        }
    }
    
    
    private void mainMenu(CharSequence text){
        try{  
            Cursor c = null;
            
            //wich line has been select ?
            if(text == getText(R.string.add_artist)) {
                c = mdb.rawQuery("SELECT artist_name _id " +
                        "FROM artist;",null);
                positionMenu = STATE.ARTIST;
            }
            else if(text == getText(R.string.add_album)) {
                c = mdb.rawQuery("SELECT album_name _id " +
                        "FROM album;",null);
                positionMenu = STATE.ALBUM;
            }
            else if(text == getText(R.string.add_song)) {
                c = mdb.rawQuery("SELECT title _id " +
                        "FROM song;",null);
                positionMenu = STATE.SONG;
            }
            else if(text == getText(R.string.add_style)) {
                c = mdb.rawQuery("SELECT genre_name _id " +
                        "FROM genre;",null);
                positionMenu = STATE.STYLE;
            }
            
            startManagingCursor(c);
            
            try{
                  ListAdapter adapter = new SimpleCursorAdapter(
                      this, R.layout.add_row, c, new String[] {"_id"},  
                      new int[] {R.id.text1});  
                  setListAdapter(adapter);
              }
              catch(Exception ex)
              {}
        }
        catch(Exception ex){Log.v(TAG, ex.toString());}
    }

}
