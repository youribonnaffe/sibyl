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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
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
    private static final int ID = 2; /*position of the column in the cursor*/
    private static final String TAG = "AddUI";
    private MusicDB mdb;    //the database

    private Cursor mCursor = null;
    private static final int[] field ={R.string.add_artist, R.string.add_album, R.string.add_style ,
            R.string.add_song, R.string.add_smart_playlist}; /* id List for main menu */
    private static final int nbField = 5; /*number of row in field*/

    private static final int[]  fieldSMP = { R.string.playlist_most_played, 
            R.string.playlist_most_played,
            R.string.playlist_random}; /*string id list for Smart Play List menu*/
    private static final int nbFieldSMP = 3;/*number of smart playlist*/

    private static final int TRANSLATION_LEFT = 0; // sense of the animation when changing menu
    private static final int TRANSLATION_RIGHT = 1;
    //private static final int TRANSLATION_NO_TRANS = 2;
    private static final class STATE {  // C++ style enum of the positions of AddUI
        public static final int MAIN    = 0;
        public static final int ARTIST  = 1;
        public static final int ALBUM   = 2;
        public static final int STYLE   = 3;
        public static final int SONG    = 4;
        public static final int SMART_PLAYLIST = 5;
    };
    private int positionMenu = STATE.MAIN; //position in the menu
    private int positionRow = 0; //row position in main menu

    private Animation RTLanim; //right to left translation
    private Animation LTRanim; //left to right translation
    
    private static final int NO_MODIF = 0; // activity has not modified the playlist
    private static final int MODIF = 1; // activity has modified the playlist
    
    @Override
    protected void onCreate(Bundle icicle) 
    {
        super.onCreate(icicle);
        Log.v(TAG,"AddUI start");
        setContentView(R.layout.add);
        LTRanim = AnimationUtils.loadAnimation(this, R.anim.ltrtranslation);
        RTLanim = AnimationUtils.loadAnimation(this, R.anim.rtltranslation);
        try
        {
            mdb = new MusicDB(this);
        }
        catch(SQLiteDiskIOException ex)
        {
            Log.v(TAG, ex.toString());
        }   
        displayMainMenu(TRANSLATION_LEFT);
        // NO_MODIF means that we didn't modify the playlist
        setResult(NO_MODIF);
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
    private void displayMainMenu(int sense)
    {
        Log.v(TAG, ">>> AddUI::displayMainMenu() called");
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.add_row, R.id.textfield);
        for( int i = 0; i < nbField; i++){ //add all strings to the adapter
            adapter.addObject(getString(field[i]));
        }
        setListAdapter(adapter);
        getListView().setSelection(positionRow);
        if( sense == TRANSLATION_LEFT) {
            getListView().startAnimation(RTLanim);
        }
        else if( sense == TRANSLATION_RIGHT) {
            getListView().startAnimation(LTRanim);
        }
    }

    /*When a row is selected, the UI is update by this method*/
    protected void onListItemClick(ListView l, View vu, int position, long id) 
    {
        refreshMenu(position);
    }


    /*AddUI is refreshed in function of where the user is the menu. The position is know with positionMenu*/
    private void refreshMenu(int pos){
        Log.v(TAG, ">>> AddUI::refreshMenu() called");
        /*LinearLayout row = (LinearLayout) vu;
        TextView text = (TextView) row.findViewById(R.id.textfield);*/
        if( positionMenu == STATE.MAIN ){
            positionRow = pos;
            showSubMenu(TRANSLATION_LEFT);/*text.getText());*/
        }
        else
        {   
            if(positionMenu == STATE.SMART_PLAYLIST){
                Music.SmartPlaylist sp = null;
                switch(fieldSMP[pos]) {
                    case R.string.playlist_less_played:
                        sp = Music.SmartPlaylist.LESS_PLAYED;
                        break;
                    case R.string.playlist_most_played:
                        sp = Music.SmartPlaylist.MOST_PLAYED;
                        break;
                    case R.string.playlist_random:
                        sp = Music.SmartPlaylist.RANDOM;
                        break;
                }
                mdb.insertPlaylist(sp);
            }
            else{
                mCursor.moveTo(pos);
                Log.v(TAG,mCursor.getString(2));
                if(positionMenu == STATE.ARTIST){
                    mdb.insertPlaylist(Music.SONG.ARTIST, mCursor.getString(ID));
                }
                else if(positionMenu == STATE.ALBUM){
                    mdb.insertPlaylist(Music.SONG.ALBUM, mCursor.getString(ID));
                }
                else if(positionMenu == STATE.SONG){
                    mdb.insertPlaylist(Music.SONG.TITLE, mCursor.getString(ID));
                }
                else if(positionMenu == STATE.STYLE){
                    mdb.insertPlaylist(Music.SONG.GENRE, mCursor.getString(ID));
                }
            }
            // MODIF means that we did modify the playlist
            setResult(MODIF);
            positionMenu = STATE.MAIN;
            displayMainMenu(TRANSLATION_LEFT);
        }

    }


    /*When a row of the main menu is selected, Addui is refreshed. And the new rows are added: list of albums, artists,... */
    private void showSubMenu(int sense){ /*CharSequence text){*/
        Log.v(TAG, ">>> AddUI::showSubMenu() called");
        if( sense == TRANSLATION_LEFT ) {
            getListView().startAnimation(RTLanim);
        }
        else if( sense == TRANSLATION_RIGHT ) {
            getListView().startAnimation(LTRanim);
        }

        //which line has been selected:  add artist, albums, songs,... except Smart Playlist
        switch( field[positionRow]){
            case R.string.add_artist: 
                mCursor = mdb.getTableList(Music.Table.ARTIST);
                positionMenu = STATE.ARTIST;
                break;

            case R.string.add_album:    
                mCursor = mdb.getTableList(Music.Table.ALBUM);
                positionMenu = STATE.ALBUM;
                break;

            case R.string.add_song:
                mCursor = mdb.getTableList(Music.Table.SONG);
                positionMenu = STATE.SONG;
                break;

            case R.string.add_style:
                mCursor = mdb.getTableList(Music.Table.GENRE);
                positionMenu = STATE.STYLE;
                break;

            case R.string.add_smart_playlist:
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.add_row, R.id.textfield);
                for( int i = 0; i < nbFieldSMP; i++){
                    adapter.addObject(getString(fieldSMP[i]));
                }

                setListAdapter(adapter);
                positionMenu = STATE.SMART_PLAYLIST;
                return; /*quit subMenu when the smart playlist row are added */
        }

        /*if the cursor is empty, we adjust the text in function of the submenu*/
        startManagingCursor(mCursor);
        if( mCursor.count() == 0 ){
            TextView emptyText = (TextView) findViewById(android.R.id.empty);
            switch(positionMenu){
                case STATE.ARTIST : 
                    emptyText.setText(R.string.add_empty_artist);
                    break;
                case STATE.ALBUM :
                    emptyText.setText(R.string.add_empty_album);
                    break;
                case STATE.STYLE :
                    emptyText.setText(R.string.add_empty_genre);
                    break;
                case STATE.SONG :
                    emptyText.setText(R.string.add_empty_song);
                    break;
            }
        }
        ListAdapter adapter = new SimpleCursorAdapter(
                this, 
                R.layout.add_row, 
                mCursor, 
                new String[] {"_id","num"},  
                new int[] {R.id.textfield, R.id.textnum});
        setListAdapter(adapter);
    }

    /*allow to use arrow in the menu*/
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        /*go deeper into menu*/
        if(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT){
            if(getListView().getSelectedItemPosition() != -1){
                refreshMenu( getListView().getSelectedItemPosition());
            }
        }

        if( keyCode == KeyEvent.KEYCODE_DPAD_LEFT ){
            /*quit AddUI*/
            if( positionMenu == STATE.MAIN ){
                finish();
            }
            /*Go upper in the menu*/
            if(positionMenu == STATE.ALBUM ||
                    positionMenu == STATE.ARTIST ||
                    positionMenu == STATE.SMART_PLAYLIST ||
                    positionMenu == STATE.SONG ||
                    positionMenu == STATE.STYLE) {
                displayMainMenu(TRANSLATION_RIGHT);
                positionMenu = STATE.MAIN;
            }
        }

        return super.onKeyUp(keyCode, event);
    }



}
