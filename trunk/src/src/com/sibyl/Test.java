package com.sibyl;

import java.io.File;
import java.io.FilenameFilter;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.util.Log;


/*
 * a example activity to illustrate database manipulation
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

	    // get all mp3 files in /tmp
	    File dir = new File("/tmp/");
	    FilenameFilter filter = new FilenameFilter() {
		public boolean accept(File dir, String name) {
		    return name.endsWith(".mp3");
		}
	    };
	    // insert them in the database
	    
	    for(String s : dir.list(filter)){
		try{
		   long t = System.currentTimeMillis();
		   mdb.insert("/tmp/"+s);
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
	    // a example to delete a song
	    /*mdb.deleteSong("/tmp/test.mp3");
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