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

import java.io.File;
import java.io.FilenameFilter;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.util.Log;


/*
 * an example activity to illustrate database manipulation
 */

// genre id3v1
// jointures id3v1

public class Test extends Activity{

    private static final String TAG = "TEST_MUSICDB";

    public void onCreate(Bundle icicle) {
	super.onCreate(icicle);

	try{
	    
	    MusicDB mdb = new MusicDB(this);
	    Log.v(TAG,"BD OK");

	    // get all mp3 files in Music.MUSIC_DIR
	    File dir = new File(Music.MUSIC_DIR+"/");
	    FilenameFilter filter = new FilenameFilter() {
		public boolean accept(File dir, String name) {
		    return name.endsWith(".mp3");
		}
	    };
	    // insert them in the database
	    
	    for(String s : dir.list(filter)){
		try{
		   long t = System.currentTimeMillis();
		   mdb.insert(Music.MUSIC_DIR+"/"+s);
		   Log.v(TAG, "temps "+(System.currentTimeMillis()-t));
		}catch(SQLiteException sqle){
		    Log.v(TAG, "sql" + sqle.toString());
		}
	    }

	    // an example to display all songs
	    // example with tables for columns names and so on
	    Cursor c = mdb.rawQuery("SELECT url, title, artist_name, album_name, genre_name " +
		    "FROM song, artist, album, genre " +
		    "WHERE artist.id = artist AND album.id=album AND genre.id=genre",null);
	    while(c.next()){
		for(String s : c.getColumnNames()){
		    Log.v(TAG, s+"="+c.getString(c.getColumnIndex(s)));
		}
		Log.v(TAG, "-----");
	    }
	    c.close();
	    
	    // an example to add song to the playlist
	    Log.v(TAG, "*** PLAYLIST ***");

	    int[] t = {1,2,3};
	    mdb.insertPlaylist(t);
	    mdb.insertPlaylist(Music.SONG.ARTIST,"The Prodigy");
	    
	    c = mdb.rawQuery("SELECT url, title, artist_name, album_name, genre_name " +
		    "FROM song, artist, album, genre, current_playlist " +
		    "WHERE artist.id = artist AND album.id=album AND genre.id=genre AND song.id = current_playlist.id",null);
	    while(c.next()){
		for(String s : c.getColumnNames()){
		    Log.v(TAG, s+"="+c.getString(c.getColumnIndex(s)));
		}
		Log.v(TAG, "-----");
	    }
	    
	    // a example to delete a song
	    /*mdb.deleteSong(Music.MUSIC_DIR+"/test.mp3");
	    mdb.deleteSong(3);
	   c = mdb.rawQuery("SELECT url, title, artist_name, album_name, genre_name " +
		    "FROM song, artist, album, genre " +
		    "WHERE artist.id = artist AND album.id=album AND genre.id=genre",null);
	    while(c.next()){
		for(String s : c.getColumnNames()){
		    Log.v(TAG, s+"="+c.getString(c.getColumnIndex(s)));
		}
		Log.v(TAG, "-----");
	    }
	    */
	}catch(Exception ex){
	    Log.v(TAG, ex.toString());
	}

    }
}