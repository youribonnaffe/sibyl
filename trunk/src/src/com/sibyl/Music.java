/* 
 *
 * Copyright (C) 2007 sibyl project
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

    public static String[] SONGS = { SONG.ID, SONG.URL, SONG.TITLE,
	    SONG.LAST_PLAYED, SONG.COUNT_PLAYED, SONG.TRACK, SONG.ARTIST,
	    SONG.ALBUM, SONG.GENRE };
    public static String[] ARTISTS = { ARTIST.ID, ARTIST.NAME };
    public static String[] ALBUMS = { ALBUM.ID, ALBUM.NAME };
    public static String[] GENRES = { GENRE.ID, GENRE.NAME };
    public static String[] CURRENT_PLAYLISTS = { CURRENT_PLAYLIST.POS,
	    CURRENT_PLAYLIST.ID };

    public static class SONG {
	public static String ID = "id";
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

}
