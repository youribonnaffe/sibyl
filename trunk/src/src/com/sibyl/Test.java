package com.sibyl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;


/*
 * a example activity to illustrate database manipulation
 */

//genre id3v1
//jointures id3v1

public class Test extends Activity{

    private static final String TAG = "TEST_MUSICDB";

    public void onCreate(Bundle icicle) {
	super.onCreate(icicle);

	setContentView(R.layout.main);

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
	    /*
	    for(String s : dir.list(filter)){
		try{
		   mdb.insert("/tmp/"+s);
		}catch(SQLiteException sqle){
		    Log.v(TAG, "sql" + sqle.toString());
		}
	    }*/

	    // an example to display all songs
	    // example with tables for columns names and so on
	    Cursor c = mdb.query("SELECT url, title, artist_name, album_name, genre_name " +
		    "FROM song, artist, album, genre " +
		    "WHERE artist.id = artist AND album.id=album AND genre.id=genre",null);
	    while(c.next()){
		for(String s : c.getColumnNames()){
		    Log.v(TAG, s+"="+c.getString(c.getColumnIndex(s)));
		}
		Log.v(TAG, "-----");
	    }

	    // a example to delete a song
	    // mdb.deleteSong("/tmp/testv1.mp3");
	    
	}catch(FileNotFoundException e){
	    Log.v(TAG, e.toString());
	}catch(Exception ex){
	    Log.v(TAG, ex.toString());
	}

    }
}