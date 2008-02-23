package com.sibyl;

import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;


// probleme url avec '''
// optimize string concat and static ?
// optimize database 

public class MusicDB {

    private SQLiteDatabase mDb;

    private static final String DB_NAME = "music.db";
    private static final int DB_VERSION = 1;

    public MusicDB(Context c) throws IOException {
	// open db or create it if needed
	try{
	    mDb = c.openDatabase(DB_NAME, null);

	    //Log.v("","open");
	}catch(FileNotFoundException fnfe){
	    mDb = c.createDatabase(DB_NAME, DB_VERSION, Context.MODE_PRIVATE, null);
	    //Log.v("","create");
	    // create all databases
	    mDb.execSQL("CREATE TABLE song("+
		    "id INTEGER PRIMARY KEY,"+
		    "url VARCHAR UNIQUE,"+
		    "title VARCHAR,"+
		    "last_played DATE DEFAULT 0,"+
		    "count_played NUMBER(5) DEFAULT 0,"+
		    "track NUMBER(2) DEFAULT 0,"+
		    "artist INTEGER,"+
		    "album INTEGER,"+
		    "genre INTEGER"+
		    ")"
	    );
	    mDb.execSQL("CREATE TABLE artist("+
		    "id INTEGER PRIMARY KEY AUTOINCREMENT,"+
		    "artist_name VARCHAR UNIQUE "+
		    ")"
	    );
	    mDb.execSQL("CREATE TABLE album("+
		    "id INTEGER PRIMARY KEY AUTOINCREMENT,"+
		    "album_name VARCHAR UNIQUE "+
		    ")"
	    );
	    mDb.execSQL("CREATE TABLE genre("+
		    "id INTEGER PRIMARY KEY AUTOINCREMENT,"+
		    "genre_name VARCHAR UNIQUE"+
		    ")"
	    );

	    mDb.execSQL("CREATE TABLE current_playlist("+
		    "pos INTEGER PRIMARY KEY,"+
		    "id INTEGER"+
		    ")"
	    );
	    // default values for undefined tags
	    mDb.execSQL("INSERT INTO artist(artist_name) VALUES('')");
	    mDb.execSQL("INSERT INTO album(album_name) VALUES('')");
	    mDb.execSQL("INSERT INTO genre(genre_name) VALUES('')");
	    // how to ensure that ?
	}
    }

    public void insert(String[] url) throws FileNotFoundException, IOException, SQLiteException{
	for(int i=0; i<url.length; i++){
	    insert(url[i]);
	}
    }

    public void insert(String url) throws FileNotFoundException, IOException, SQLiteException{

	ContentValues cv = new ID3TagReader(url).getValues();
	// 
	Log.v("CV", cv.toString());
	int artist = 0 ,album = 0, genre = 0; // = 0 -> last value, !=0 -> null or select
	//, album = false, genre = false;
	mDb.execSQL("BEGIN TRANSACTION");
	try{
	    if(cv.containsKey(Music.ARTIST.NAME)){
		try{
		    mDb.execSQL("INSERT INTO artist(artist_name) VALUES('"+cv.getAsString(Music.ARTIST.NAME)+"')");
		    artist = 0;
		}catch(SQLiteException sqle){
		    Cursor c = mDb.rawQuery("SELECT id FROM artist WHERE artist_name='"+cv.getAsString(Music.ARTIST.NAME)+"'" ,null);
		    if(c.next()){
			artist = c.getInt(0);
		    }
		    c.close();
		}
	    }else{
		artist = 1;
	    }

	    if(cv.containsKey(Music.ALBUM.NAME)){
		try{
		    mDb.execSQL("INSERT INTO album(album_name) VALUES('"+cv.getAsString(Music.ALBUM.NAME)+"')");
		    album = 0;
		}catch(SQLiteException sqle){
		    Cursor c = mDb.rawQuery("SELECT id FROM album WHERE album_name='"+cv.getAsString(Music.ALBUM.NAME)+"'" ,null);
		    if(c.next()){
			album = c.getInt(0);
		    }
		    c.close();
		}
	    }else{
		album = 1;
	    }

	    if(cv.containsKey(Music.GENRE.NAME)){
		try{
		    mDb.execSQL("INSERT INTO genre(genre_name) VALUES('"+cv.getAsString(Music.GENRE.NAME)+"')");
		    genre = 0;
		}catch(SQLiteException sqle){
		    Cursor c = mDb.rawQuery("SELECT id FROM genre WHERE genre_name='"+cv.getAsString(Music.GENRE.NAME)+"'" ,null);
		    if(c.next()){
			genre = c.getInt(0);
		    }
		    c.close();
		}
	    }else{
		genre = 1;
	    }

	    // insert order in table song
	    mDb.execSQL("INSERT INTO song(url,title,track, artist,album,genre) VALUES('"+url+"','"+
		    (cv.containsKey(Music.SONG.TITLE) ? cv.getAsString(Music.SONG.TITLE) : "")+"','"+
		    (cv.containsKey(Music.SONG.TRACK) ? cv.getAsString(Music.SONG.TRACK) : "")+"',"+
		    (artist != 0 ? artist : "(SELECT max(id) FROM artist)")+","+
		    (album != 0 ? album : "(SELECT max(id) FROM album)")+","+
		    (genre != 0 ? genre : "(SELECT max(id) FROM genre)")+")");
	    // + 1 a cause du commit
	    mDb.execSQL("COMMIT TRANSACTION");

	}catch(SQLiteException e){
	    mDb.execSQL("ROLLBACK");
	    throw e;
	}
    }

    public void deleteSong(String url){
	try{
        	mDb.execSQL("BEGIN TRANSACTION");
        
        	if(mDb.rawQuery("SELECT COUNT(*) FROM SONG WHERE artist=(SELECT artist FROM song WHERE url='"+url+"')",null).count()==1){
        	    mDb.execSQL("DELETE FROM artist WHERE id=(SELECT artist FROM song WHERE url='"+url+"')");
        	}
        	if(mDb.rawQuery("SELECT COUNT(*) FROM SONG WHERE album=(SELECT album FROM song WHERE url='"+url+"')",null).count()==1){
        	    mDb.execSQL("DELETE FROM album WHERE id=(SELECT album FROM song WHERE url='"+url+"')");
        	}
        	if(mDb.rawQuery("SELECT COUNT(*) FROM SONG WHERE genre=(SELECT genre FROM song WHERE url='"+url+"')",null).count()==1){
        	    mDb.execSQL("DELETE FROM genre WHERE id=(SELECT genre FROM song WHERE url='"+url+"')");
        	}
        	mDb.execSQL("DELETE FROM song WHERE url='"+url+"'");
        
        	mDb.execSQL("COMMIT TRANSACTION");
	}catch(SQLiteException e){
	    mDb.execSQL("ROLLBACK");
	    throw e;
	}
    }

    public void deleteSong(int id){
	try{
    	mDb.execSQL("BEGIN TRANSACTION");
    
    	if(mDb.rawQuery("SELECT COUNT(*) FROM SONG WHERE artist=(SELECT artist FROM song WHERE id='"+id+"')",null).count()==1){
    	    mDb.execSQL("DELETE FROM artist WHERE id=(SELECT artist FROM song WHERE id='"+id+"')");
    	}
    	if(mDb.rawQuery("SELECT COUNT(*) FROM SONG WHERE album=(SELECT album FROM song WHERE id='"+id+"')",null).count()==1){
    	    mDb.execSQL("DELETE FROM album WHERE id=(SELECT album FROM song WHERE id='"+id+"')");
    	}
    	if(mDb.rawQuery("SELECT COUNT(*) FROM SONG WHERE genre=(SELECT genre FROM song WHERE id='"+id+"')",null).count()==1){
    	    mDb.execSQL("DELETE FROM genre WHERE id=(SELECT genre FROM song WHERE id='"+id+"')");
    	}
    	mDb.execSQL("DELETE FROM song WHERE id='"+id+"'");
    
    	mDb.execSQL("COMMIT TRANSACTION");
	}catch(SQLiteException e){
	    mDb.execSQL("ROLLBACK");
	    throw e;
	}
    }

    public Cursor rawQuery(String sql, String[] selectionArgs){
	return mDb.rawQuery(sql, selectionArgs);
    }

    public Cursor query(String table, String[] columns, 
	    String selection, String[] selectionArgs, 
	    String groupBy, String having, String orderBy){
	return mDb.query(table, columns, selection, selectionArgs, groupBy, having, orderBy);
    }
    
    public Cursor query(boolean distinct, String table, String[] columns, 
	    String selection, String[] selectionArgs, 
	    String groupBy, String having, String orderBy){
	return mDb.query(distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy);
    }
    
    public String nextSong(int pos){
	Cursor c = mDb.rawQuery("SELECT url FROM song, current_playlist WHERE pos="+pos+"+1 AND song.id=current_playlist.id", null);
	if(!c.first()){
	    return null;
	}
	return c.getString(0);
    }
    
    public String previousSong(int pos){
	Cursor c = mDb.rawQuery("SELECT url FROM song, current_playlist WHERE pos="+pos+"-1 AND song.id=current_playlist.id", null);
	if(!c.first()){
	    return null;
	}
	return c.getString(0);
    }
    
    public void execSQL(String query){
	mDb.execSQL(query);
    }
}
