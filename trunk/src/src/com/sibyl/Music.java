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

public class Music {
    
    /* printed name of the application */
    public static String APPNAME = "Sibyl";
    
   public static final String PREFS = "sibyl_prefs";
    
    /* states of the service */
    public static class State {
        public static int ERROR = -1;
        public static int PLAYING = 0;
        public static int PAUSED = 1;
        public static int STOPPED = 2;
        public static int END_PLAYLIST_REACHED = 0x10;
    }
    
    /* Modes of the service */
    public static class Mode {
        public static int NORMAL = 1; // songs are played in the order of the playlist
        public static int RANDOM = 2; // songs are played randomly
    }
    
    public static class LoopMode {
        public static int NO_REPEAT = 0; // each song will be played once
        public static int REPEAT_SONG = 1; // the current song will be repeated while loopmode is REPEAT_SONG 
        public static int REPEAT_PLAYLIST = 2; // the current playlist will be repeated when finished  
    }
    
    /* other stuff to be commented */
    public static enum Table { 
        SONG, ARTIST, GENRE, ALBUM, CURRENT_PLAYLIST, DIR; 
    }
    public static String MUSIC_DIR = "/data/music";
    public static String[] SONGS = { SONG.ID, SONG.URL, SONG.TITLE,
	SONG.LAST_PLAYED, SONG.COUNT_PLAYED, SONG.TRACK, SONG.ARTIST,
	SONG.ALBUM, SONG.GENRE };
    public static String[] ARTISTS = { ARTIST.ID, ARTIST.NAME };
    public static String[] ALBUMS = { ALBUM.ID, ALBUM.NAME };
    public static String[] GENRES = { GENRE.ID, GENRE.NAME };
    public static String[] CURRENT_PLAYLISTS = { CURRENT_PLAYLIST.POS,
	CURRENT_PLAYLIST.ID };
    public static String[] DIR = {DIRECTORY.ID, DIRECTORY.DIR};

    public static class SONG {
	public static String ID = "_id";
	public static String URL = "url";
	public static String TITLE = "title";
	public static String LAST_PLAYED = "last_played";
	public static String COUNT_PLAYED = "count_played";
	public static String TRACK = "track";
	public static String ARTIST = "artist";
	public static String ALBUM = "album";
	public static String GENRE = "genre";
    }

    public static class ARTIST {
	public static String ID = "id";
	public static String NAME = "artist_name";
    }

    public static class ALBUM {
	public static String ID = "id";
	public static String NAME = "album_name";
    }

    public static class GENRE {
	public static String ID = "id";
	public static String NAME = "genre_name";
    }

    public static class CURRENT_PLAYLIST {
	public static String POS = "pos";
	public static String ID = "id";
    }

    public static class DIRECTORY 
    {
	public static String ID = "id_";
	public static String DIR = "dir";
    }

    // describe playlist possiblities
    public static enum SmartPlaylist
    {
	RANDOM, LESS_PLAYED, MOST_PLAYED;
	
	// SQL query associated with a playlist
	public String getQuery(){
	    switch(this){
    	    	case RANDOM : 
    	    	    return "INSERT INTO current_playlist(id) SELECT _id FROM song ORDER BY random() LIMIT 25";
    	    	case LESS_PLAYED : 
    	    	    return "INSERT INTO current_playlist(id) SELECT _id FROM song ORDER BY count_played ASC LIMIT 25";
    	    	case MOST_PLAYED :
    	    	    return "INSERT INTO current_playlist(id) SELECT _id FROM song ORDER BY count_played DESC LIMIT 25";
    	    	default : 
    	    	    return "";
	    }
	}
	
	// get string id for the enum values
	public int getStringId(){
	    switch(this){
	    	case RANDOM : 
	    	    return R.string.playlist_random;
	    	case LESS_PLAYED : 
	    	    return R.string.playlist_less_played;
	    	case MOST_PLAYED :
	    	    return R.string.playlist_most_played;
	    	default :
	    	    return android.R.string.unknownName;
	    }
	}
    }

}
