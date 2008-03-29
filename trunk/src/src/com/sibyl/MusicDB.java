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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;

//optimize string concat and static ?
//optimize database -> if needed try triggers

public class MusicDB {

    private SQLiteDatabase mDb;

    private static final String DB_NAME = "music.db";
    private static final int DB_VERSION = 1;
    private static final int NB_PREDEFINED_GENRE = 149;
    
    private static class MusicDBHelper extends SQLiteOpenHelper{
	private Context usedC;
	@Override
	public void onCreate(SQLiteDatabase mDb) {
	    mDb.execSQL("CREATE TABLE song("+
		    "_id INTEGER PRIMARY KEY,"+
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

	    mDb.execSQL("CREATE TABLE directory("+
		    "_id INTEGER PRIMARY KEY AUTOINCREMENT,"+
		    "dir VARCHAR UNIQUE"+
		    ")"
	    );
	    // triggers
	    mDb.execSQL("CREATE TRIGGER t_del_song_artist " +
		    "AFTER DELETE ON song " +
		    "FOR EACH ROW " +
		    "WHEN ( OLD.artist!=1 AND (SELECT COUNT(*) FROM SONG WHERE artist=OLD.artist) == 0) " +
		    "BEGIN " +
		    "DELETE FROM artist WHERE id=OLD.artist; " +
	    "END;");
	    mDb.execSQL("CREATE TRIGGER t_del_song_album " +
		    "AFTER DELETE ON song " +
		    "FOR EACH ROW " +
		    "WHEN ( OLD.album!=1 AND (SELECT COUNT(*) FROM SONG WHERE album=OLD.album) == 0) " +
		    "BEGIN " +
		    "DELETE FROM album WHERE id=OLD.album; " +
	    "END;");
	    mDb.execSQL("CREATE TRIGGER t_del_song_genre " +
		    "AFTER DELETE ON song " +
		    "FOR EACH ROW " +
		    "WHEN ( OLD.genre>"+NB_PREDEFINED_GENRE+" AND (SELECT COUNT(*) FROM SONG WHERE genre=OLD.genre) == 0) " +
		    "BEGIN " +
		    "DELETE FROM genre WHERE id=OLD.genre; " +
	    "END;");

	    // default values for undefined tags
	    mDb.execSQL("INSERT INTO artist(artist_name) VALUES('')");
	    mDb.execSQL("INSERT INTO album(album_name) VALUES('')");
	    mDb.execSQL("INSERT INTO genre(genre_name) VALUES('')");
	    // read id3v1 genres from file
	    try{
		// kind of boring ...
		BufferedReader f = new BufferedReader(
			new InputStreamReader(
				usedC.getResources().openRawResource(R.raw.tags)),8192);
		String line;
		while( (line=f.readLine()) != null){
		    mDb.execSQL(line);
		}
	    }catch(FileNotFoundException fnfe){
		// todo ?
	    }catch(IOException ioe){
		// todo ?
	    }

	    // how to ensure that ?
	}

	@Override
	public void onUpgrade(SQLiteDatabase mDb, int oldVersion, int newVersion) {
	    // drop and re-create tables
	    mDb.execSQL("DROP TABLE IF EXISTS current_playlist");
	    mDb.execSQL("DROP TABLE IF EXISTS genre");
	    mDb.execSQL("DROP TABLE IF EXISTS album");
	    mDb.execSQL("DROP TABLE IF EXISTS artist");
	    mDb.execSQL("DROP TABLE IF EXISTS song");
	    mDb.execSQL("DROP TABLE IF EXISTS directory");
	    mDb.execSQL("DROP TRIGGER IF EXISTS t_del_song_genre");
	    mDb.execSQL("DROP TRIGGER IF EXISTS t_del_song_artist");
	    mDb.execSQL("DROP TRIGGER IF EXISTS t_del_song_album");
	    onCreate(mDb);
	}

	public SQLiteDatabase openDatabase(Context context, String name, CursorFactory factory, int newVersion){
	    usedC = context;
	    return super.openDatabase(context, name, factory, newVersion);
	}
    }

    /**
     * constructor to get a connection to the database, if the database doesn't
     * exist yet, it is created
     * @param c the application context
     */
    public MusicDB( Context c ) { 
	// exceptions ??
	mDb = (new MusicDBHelper()).openDatabase(c, DB_NAME, null, DB_VERSION);
    }

    /**
     * insert songs in the database where urls are the absolute filenames of them
     * the files are processed to extract the ID3 tags
     * 
     * @param url array of urls to insert several songs at a time
     * @throws FileNotFoundException
     * @throws IOException
     * @throws SQLiteException
     */
    public void insert(String[] urls) throws FileNotFoundException, IOException, SQLiteException{
	for(int i=0; i<urls.length; i++){
	    insert(urls[i]);
	}
    }

    /**
     * insert one song in the database where url is the absolute filename of it
     * the file is processed to extract the ID3 tags
     * 
     * @param url the absolute path (and name) of the song
     * @throws FileNotFoundException
     * @throws IOException
     * @throws SQLiteException
     */
    public void insert(String url) throws FileNotFoundException, IOException, SQLiteException{
	// read tags
	ContentValues cv = new ID3TagReader(url).getValues();

	int artist = 0 ,album = 0, genre = 0; // = 0 -> last value, !=0 -> null or select
	//, album = false, genre = false;
	mDb.execSQL("BEGIN TRANSACTION");
	try{
	    if(cv.containsKey(Music.ARTIST.NAME) && cv.getAsString(Music.ARTIST.NAME) != ""){
		Cursor c = mDb.rawQuery("SELECT id FROM artist WHERE artist_name='"+cv.getAsString(Music.ARTIST.NAME)+"'" ,null);
		if(c.next()){
		    artist = c.getInt(0);
		}else{
		    mDb.execSQL("INSERT INTO artist(artist_name) VALUES('"+cv.getAsString(Music.ARTIST.NAME)+"')");
		}
		c.close();
	    }else{
		artist = 1;
	    }

	    if(cv.containsKey(Music.ALBUM.NAME) && cv.getAsString(Music.ALBUM.NAME) != ""){
		Cursor c = mDb.rawQuery("SELECT id FROM album WHERE album_name='"+cv.getAsString(Music.ALBUM.NAME)+"'" ,null);
		if(c.next()){
		    album = c.getInt(0);
		}else{
		    mDb.execSQL("INSERT INTO album(album_name) VALUES('"+cv.getAsString(Music.ALBUM.NAME)+"')");
		}
		c.close();
	    }else{
		album = 1;
	    }

	    if(cv.containsKey(Music.GENRE.NAME) && cv.getAsString(Music.GENRE.NAME) != ""){
		Cursor c = mDb.rawQuery("SELECT id FROM genre WHERE genre_name='"+cv.getAsString(Music.GENRE.NAME)+"'" ,null);
		if(c.next()){
		    genre = c.getInt(0);
		}else{
		    mDb.execSQL("INSERT INTO genre(genre_name) VALUES('"+cv.getAsString(Music.GENRE.NAME)+"')");
		}
		c.close();
	    }else{
		genre = 1;
	    }

	    // insert order in table song
	    // char ' is protected in url, for the tags this is done during reading them
	    mDb.execSQL("INSERT INTO song(url,title,track, artist,album,genre) VALUES('"+
		    url.replace("'", "''")+"','"+
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

    /** 
     * delete a song from the database with the url given
     * @param url
     */
    public void deleteSong(String url){
	mDb.execSQL("DELETE FROM song WHERE url='"+url+"'");
    }

    /**
     * delete a song from the database with the url given
     * @param id
     */
    public void deleteSong(int id){
	mDb.execSQL("DELETE FROM song WHERE _id='"+id+"'");
    }

    /**
     * just to allow the user to execute query on the database 
     * @see android.database.sqlite.SQLiteDatabase#rawQuery(String, String[])
     * @param sql the SQL query
     * @param selectionArgs see android doc
     * @return
     */
    public Cursor rawQuery(String sql, String[] selectionArgs){
	return mDb.rawQuery(sql, selectionArgs);
    }

    /**
     * to execute query each arg is a part of the SQL query
     * 
     * @param table
     * @param columns
     * @param selection
     * @param selectionArgs
     * @param groupBy
     * @param having
     * @param orderBy
     * @return
     */
    public Cursor query(String table, String[] columns, 
	    String selection, String[] selectionArgs, 
	    String groupBy, String having, String orderBy){
	return mDb.query(table, columns, selection, selectionArgs, groupBy, having, orderBy);
    }

    /**
     * to execute query each arg is a part of the SQL query
     * @param distinct
     * @param table
     * @param columns
     * @param selection
     * @param selectionArgs
     * @param groupBy
     * @param having
     * @param orderBy
     * @return
     */
    public Cursor query(boolean distinct, String table, String[] columns, 
	    String selection, String[] selectionArgs, 
	    String groupBy, String having, String orderBy){
	return mDb.query(distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy);
    }

    /**
     * return the song at pos in the playlist
     * @param pos
     * @return url of the next song
     */
    public String getSong(int pos){
	Cursor c = mDb.rawQuery("SELECT url FROM song, current_playlist WHERE pos="+pos+" AND song._id=current_playlist.id", null);
	if(!c.first()){
	    return null;
	}
	return c.getString(0);
    }

    /**
     * execute SQL statement that is not a query (INSERT, DELETE ...)
     * 
     * @see android.database.sqlite.SQLiteDatabase#execSQL(String)
     * @param query
     */
    public void execSQL(String query){
	mDb.execSQL(query);
    }


    /**
     * insert several songs in the playlist
     * 
     * @param ids the songs ids
     */
    public void insertPlaylist( int[] ids ){
	for( int id : ids ){
	    mDb.execSQL("INSERT INTO current_playlist(id) VALUES("+id+")");
	}
    }

    // add song where column = value
    // use column name from Music.SONG 
    // artist, album and genre implemented
    /**
     * add song where column = value
     * use column name from Music.SONG 
     * artist, album and genre implemented
     * 
     * @param column the selected column
     * @param value  the value for the selected column
     */
    public void insertPlaylist(String column, String value){
	if(column == Music.SONG.ARTIST){
	    mDb.execSQL("INSERT INTO current_playlist(id) " +
		    "SELECT song._id FROM song, artist " +
		    "WHERE artist.artist_name = \""+value+"\"" +
	    "AND song.artist = artist.id");
	}else if(column == Music.SONG.ALBUM){
	    mDb.execSQL("INSERT INTO current_playlist(id) " +
		    "SELECT song._id FROM song, album " +
		    "WHERE album.album_name = \""+value+"\"" +
	    "AND song.album = album.id");
	}else if(column == Music.SONG.GENRE){
	    mDb.execSQL("INSERT INTO current_playlist(id) " +
		    "SELECT song._id FROM song, genre " +
		    "WHERE genre.genre_name =\""+value+"\"" +
	    "AND song.genre = genre.id");
	}else if(column == Music.SONG.TITLE){
	    mDb.execSQL("INSERT INTO current_playlist(id) " +
		    "SELECT song._id FROM song " +
		    "WHERE song.title=\""+value+"\"");
	}
    }
    
    public void insertPlaylist(Music.SmartPlaylist sp){
	mDb.execSQL(sp.getQuery());
    }

    public String[] getSongInfoFromCP(int i)
    {
	Cursor c = mDb.rawQuery("SELECT title, artist_name FROM song, current_playlist, artist "
		+"WHERE pos="+i+" AND song._id=current_playlist.id and song.artist=artist.id", null);
	if(!c.first()){
	    return null;
	}
	String str[] = {c.getString(0), c.getString(1)};
	return str;
    }

    public Cursor getPlayListInfo()
    {
	return mDb.rawQuery("SELECT title _id, artist_name FROM song, current_playlist, artist "
		+"WHERE song._id=current_playlist.id and song.artist=artist.id", null);

    }

    public void clearPlaylist()
    {
	mDb.execSQL("DELETE FROM current_playlist");
    }

    public void insertDir(String dir)
    {
	Cursor c = mDb.rawQuery("SELECT dir FROM directory WHERE dir='"+dir+"'",null);
	if (c.count() == 0)
	{
	    mDb.execSQL("INSERT INTO directory(dir) VALUES('"+dir+"')");
	}
    }
    public Cursor getDir()
    {
	return mDb.rawQuery("SELECT dir FROM directory",null);
    }
    public void delDir(String id)
    {
	mDb.execSQL("DELETE FROM directory WHERE _id="+id);
    }
    public void clearDB()
    {
	mDb.execSQL("DELETE FROM song");
	mDb.execSQL("DELETE FROM artist");
	mDb.execSQL("DELETE FROM album");
	mDb.execSQL("DELETE FROM genre");
    }
}
