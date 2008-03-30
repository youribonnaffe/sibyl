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
import android.database.sqlite.SQLiteDiskIOException;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
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
    
    private enum STATE { MAIN, ARTIST, ALBUM, STYLE,SONG, SMART_PLAYLIST};

    
    private STATE positionMenu = STATE.MAIN; //position in the menu

    @Override
    protected void onCreate(Bundle icicle) 
    {
        super.onCreate(icicle);
        setContentView(R.layout.add);
        try
        {
            mdb = new MusicDB(this);
            Log.v(TAG,"BD OK");
        }
        catch(SQLiteDiskIOException ex)
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
    
    /*display the main menu of AddUI. Where you can choose the adding mode: by artist, song, album,...*/
    private void displayMainMenu()
    {
        String[] field = {getString(R.string.add_artist),
                getString(R.string.add_album),
                getString(R.string.add_song),
                getString(R.string.add_style),
                getString(R.string.add_smart_playlist)};

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.add_row, R.id.text1, field);
        setListAdapter(adapter);      
    }
    
    /*When a row is selected, the UI is update by this method*/
    protected void onListItemClick(ListView l, View vu, int position, long id) 
    {
        refreshMenu(vu);
    }
    
    
    /*AddUI is refreshed in function of where the user is the menu. The position is know with positionMenu*/
    private void refreshMenu(View vu){
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
            else if(positionMenu == STATE.SMART_PLAYLIST){
                Music.SmartPlaylist sp = null;
                String spSelected = text.getText().toString();
                if(spSelected.equals(getString(R.string.playlist_less_played))){
                    sp = Music.SmartPlaylist.LESS_PLAYED;
                }
                if(spSelected.equals(getString(R.string.playlist_most_played))){
                    sp = Music.SmartPlaylist.MOST_PLAYED;
                }
                if(spSelected.equals(getString(R.string.playlist_random))){
                    sp = Music.SmartPlaylist.RANDOM;
                }
                mdb.insertPlaylist(sp);
            }
            positionMenu = STATE.MAIN;
            displayMainMenu();
            Log.v(TAG,text.toString());
        }
        
    }
    
    
    /* TODO C'est moche les requetes faites directement par l'activity... */
    
    /*When a row of the main menu is selected, Addui is refreshed. And the new rows are added: list of albums, artists,... */
    private void mainMenu(CharSequence text){
        Cursor c = null;
            
        //wich line has been selected:  add artist, albums, songs,... except Smart Playlist
        if(text == getText(R.string.add_artist)) {
            c = mdb.rawQuery("SELECT artist_name _id " +
                    "FROM artist, song WHERE "+ Music.SONG.ARTIST + " = " + Music.ARTIST.ID + " ORDER BY artist_name;",null);
            positionMenu = STATE.ARTIST;
        }
        else if(text == getText(R.string.add_album)) {
            c = mdb.rawQuery("SELECT album_name _id " +
                    "FROM album, song WHERE " + Music.ALBUM.ID +" = "+Music.SONG.ALBUM +" ORDER BY album_name;",null);
            positionMenu = STATE.ALBUM;
        }
        else if(text == getText(R.string.add_song)) {
            c = mdb.rawQuery("SELECT title _id " +
                    "FROM song ORDER BY title;",null);
            positionMenu = STATE.SONG;
        }
        else if(text == getText(R.string.add_style)) {
            c = mdb.rawQuery("SELECT DISTINCT genre_name _id " +
                    "FROM genre,song WHERE " + Music.GENRE.ID + " = "+ Music.SONG.GENRE +" ORDER BY genre_name;",null);
            positionMenu = STATE.STYLE;
        }
        else if(text == getText(R.string.add_smart_playlist)) {
            String[] field = {getString(R.string.playlist_most_played),
                    getString(R.string.playlist_most_played),
                    getString(R.string.playlist_random)};
            
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.add_row, R.id.text1, field);
            setListAdapter(adapter);
            
            positionMenu = STATE.SMART_PLAYLIST;
            return; /*quit mainMenu when the smart playlist row are added */
            }
                    
        /*if the cursor is empty, we adjust the text in function of the submenu*/
        startManagingCursor(c);
        if( c.count() == 0){
            TextView emptyText = (TextView) findViewById(android.R.id.empty);
            if( positionMenu == STATE.ARTIST){
                emptyText.setText(R.string.add_empty_artist);
            }
            if( positionMenu == STATE.ALBUM){
                emptyText.setText(R.string.add_empty_album);
            }
            if( positionMenu == STATE.SONG || positionMenu == STATE.STYLE){
                emptyText.setText(R.string.add_empty_album);
            }
        }
        ListAdapter adapter = new SimpleCursorAdapter(
        this, R.layout.add_row, c, new String[] {"_id"},  
        new int[] {R.id.text1});  
        setListAdapter(adapter);
    }

    /*allow to use arrow in the menu*/
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT){
            Log.v(TAG,"KEY UP");
            refreshMenu( getListView().getSelectedView());
        }

        if( keyCode == KeyEvent.KEYCODE_DPAD_LEFT){
            /*quit AddUI*/
            if( positionMenu == STATE.MAIN){
                finish();
            }
            /*Go deeper in the menu*/
            if(positionMenu == STATE.ALBUM ||
                    positionMenu == STATE.ARTIST ||
                    positionMenu == STATE.SMART_PLAYLIST ||
                    positionMenu == STATE.SONG ||
                    positionMenu == STATE.STYLE) {
                displayMainMenu();
                positionMenu = STATE.MAIN;
            }

        }        
        return super.onKeyUp(keyCode, event);
    }
    
    

}
