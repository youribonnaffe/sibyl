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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;

//notes
//optimize string concat and static ?
//optimize database -> if needed try triggers


/**
 * Connection to the database
 * 
 * When deleting a song from the database, you just need to delete it from the
 * table song, triggers will do for other tables
 * 
 * When querying for data:
 * - if returned value is a cursor, you'll have to check
 * the cursor content
 * - if returned value is a String, you'll have to check if it isn't null
 *  
 * @author sibyl
 */
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
                    "album_name VARCHAR UNIQUE, "+
                    "cover_url VARCHAR DEFAULT NULL"+
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
            mDb.execSQL("CREATE TRIGGER t_del_song_current_playlist " +
                    "AFTER DELETE ON song " +
                    "FOR EACH ROW " +
                    "BEGIN " +
                    "DELETE FROM current_playlist WHERE id=OLD._id; " +
            "END;");

            // default values for undefined tags
            mDb.execSQL("INSERT INTO artist(artist_name) VALUES('"+usedC.getString(R.string.tags_unknown)+"')");
            mDb.execSQL("INSERT INTO album(album_name) VALUES('"+usedC.getString(R.string.tags_unknown)+"')");
            mDb.execSQL("INSERT INTO genre(genre_name) VALUES('"+usedC.getString(R.string.tags_unknown)+"')");
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
                // todo ? should we fill the database with unknow tags ?
            }catch(IOException ioe){
                // todo ?
            }

            // how to ensure that ?

            //reset preferences because the playlist is deleted
            usedC.getSharedPreferences(Music.PREFS, Context.MODE_PRIVATE).edit().clear();
            usedC.getSharedPreferences(Music.PREFS, Context.MODE_PRIVATE).edit().commit();
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
            mDb.execSQL("DROP TRIGGER IF EXISTS t_del_song_current_playlist");
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
    public MusicDB( Context c ) throws SQLiteDiskIOException { 
        // exceptions ??
        mDb = (new MusicDBHelper()).openDatabase(c, DB_NAME, null, DB_VERSION);
        if( mDb == null ){
            throw new SQLiteDiskIOException("Error when creating database");
        }
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
            if(cv.containsKey(Music.ARTIST.NAME) && cv.getAsString(Music.ARTIST.NAME).length() > 0){
                Log.v("INSERTION ARTISTE", "-"+cv.getAsString(Music.ARTIST.NAME).length()+"-");
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

            if(cv.containsKey(Music.ALBUM.NAME) && cv.getAsString(Music.ALBUM.NAME).length() > 0){
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

            if(cv.containsKey(Music.GENRE.NAME) && cv.getAsString(Music.GENRE.NAME).length() > 0){
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
            Log.v("debug",cv.toString());
            // insert order in table song
            String title;
            if(cv.containsKey(Music.SONG.TITLE) && cv.getAsString(Music.SONG.TITLE).length() > 0){
                title = cv.getAsString(Music.SONG.TITLE);
            }else{
                title = new File(url).getName();
            }
            // char ' is protected in url, for the tags this is done during reading them
            mDb.execSQL("INSERT INTO song(url,title,track, artist,album,genre) VALUES('"+
                    url.replace("'", "''")+"','"+
                    title +"','"+
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
     * shouldn't throw SQLException since the query is right
     * @param url
     */
    public void deleteSong(String url){ 
        mDb.execSQL("DELETE FROM song WHERE url='"+url+"'");
    }

    /**
     * delete a song from the database with the url given
     * shouldn't throw SQLException since the query is right
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
     * DEPRECATED
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
     * the song ids are not verified !
     * @param ids the songs ids
     */
    public void insertPlaylist( int[] ids ){
        for( int id : ids ){
            mDb.execSQL("INSERT INTO current_playlist(id) VALUES("+id+")");
        }
    }

    /**
     * add song where column = value
     * use column name from Music.SONG 
     * artist, album and genre implemented
     * 
     * @param column the selected column
     * @param value  the value for the selected column
     */
    public void insertPlaylist(String column, String valId){
        if(column == Music.SONG.ARTIST){
            mDb.execSQL("INSERT INTO current_playlist(id) " +
                    "SELECT song._id FROM song " +
                    "WHERE song.artist = " + valId);
        }else if(column == Music.SONG.ALBUM){
            mDb.execSQL("INSERT INTO current_playlist(id) " +
                    "SELECT song._id FROM song " +
                    "WHERE song.album = "+valId);
        }else if(column == Music.SONG.GENRE){
            mDb.execSQL("INSERT INTO current_playlist(id) " +
                    "SELECT song._id FROM song " +
                    "WHERE song.genre = "+valId);
        }else if(column == Music.SONG.TITLE){
            mDb.execSQL("INSERT INTO current_playlist(id) " +
                    "SELECT song._id FROM song " +
                    "WHERE song._id= "+valId);
        }
    }

    /**
     * @param sp the smartplaylist you want to load in the current playlist
     * @see com.sibyl.Music
     */
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

    public void delDir(String dir)
    {
        mDb.execSQL("DELETE FROM directory WHERE dir='"+dir+"'");
    }

    public void clearDB()
    {
        mDb.execSQL("DELETE FROM song");
        mDb.execSQL("DELETE FROM song");
    }

    public void countUp(int i)
    {
        Cursor c = mDb.rawQuery("SELECT count_played, song._id FROM song, current_playlist WHERE song._id=current_playlist.id and current_playlist.pos='"+i+"'",null);
        if(c.first())
        {
            int nb = c.getInt(0);
            nb++;
            String id = c.getString(1);
            mDb.execSQL("UPDATE song SET count_played="+nb+" WHERE _id='"+id+"'");
            //Log.v("MusicDB","nb d'execution :"+nb+", chanson :"+id);
        }
    }
    
    /**
     * Returns the size of the current playlist
     *
     * @return  size of the playlist or -1 if an error occurs
     */
    public int getPlaylistSize()
    {
        int size = 0;
        Cursor c = mDb.rawQuery("SELECT COUNT(id) FROM current_playlist" ,null);
        if(c.first())
        {
            size = c.getInt(0);
        }
        return size;
    }

    /**
     * to get all artists, genres, albums and songs with the count of it
     * @return Cursor or null if query is undefined
     * @see com.sibyl.ui.AddUI
     */
    public Cursor getTableList(Music.Table table){
        switch(table){
        case SONG :
            return mDb.rawQuery("SELECT title _id, ' ' num , song._id id "+
                    "FROM song "+
                    "ORDER BY title",null);
        case ALBUM :
            return mDb.rawQuery("SELECT album_name _id, '( ' || COUNT(*) || ' )' num, album.id id " +
                    "FROM album, song "+
                    "WHERE id = album "+
                    "GROUP BY album_name "+
                    "ORDER BY album_name",null);
        case ARTIST :
            return mDb.rawQuery("SELECT artist_name _id, '( ' || COUNT(*) || ' )' num, artist.id id " +
                    "FROM artist, song " +
                    "WHERE id = artist "+
                    "GROUP BY artist_name " +
                    "ORDER BY artist_name",null);
        case GENRE :
            return mDb.rawQuery("SELECT DISTINCT genre_name _id, '( ' || COUNT(*) || ' )' num, genre.id id " +
                    "FROM genre,song "+
                    "WHERE id = genre "+
                    "GROUP BY genre_name "+
                    "ORDER BY genre_name ",null);
        default : return null;
        }
    }
    
    /**
     * 
     * @param albumId
     * @return 0 : album name, 1 : artist id, 2 : artist name
     */
    public Cursor getAlbumInfo(int albumId){
        // because 1 is for unknown songs
        if(albumId == 1) return null;
        
        return mDb.rawQuery("SELECT album_name, artist.id as id, artist_name " +
        		"FROM song, album, artist " +
        		"WHERE album.id=album " +
        		"AND artist.id=artist " +
        		"AND album.id="+albumId+
        		" GROUP BY artist_name", null);
    }
    
    public void setCover(int albumId, String cover){
        if(albumId >= 1){
            mDb.execSQL("UPDATE album SET cover_url='"+cover+"' WHERE "+albumId+" =album.id");
        }
    }


}

